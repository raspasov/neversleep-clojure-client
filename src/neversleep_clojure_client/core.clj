(ns neversleep-clojure-client.core
  (:require [neversleep-clojure-client.aleph-netty :as aleph-netty]
            [clojure.core.async :refer [<!! >!! go <! >! alts! chan thread timeout pipe close!]]
            [clojure.core.async.impl.protocols :refer [closed?]]
            [manifold.stream :as s]
            [cheshire.core :as cheshire]
            [neversleep-clojure-client.time :as time]
            [neversleep-clojure-client.util :as util])
  (:import (jv SystemClock)
           (clojure.lang IFn)
           (clojure.core.async.impl.channels MMC))
  (:gen-class))

(def ^:const api-version 1)

(def ^:const latest-server-timestamp "___________________")

(def ^:const end-of-times "0000000000000000000")

(def client (atom nil))

(defn connect-stream-to-core-async-channels [s]
  (let [stream-in-ch (chan 1)
        stream-out-ch (chan 1)]
    (s/connect s stream-in-ch)
    (s/connect stream-out-ch s)
    {:in stream-in-ch
     :out stream-out-ch}))

(def pending-requests (atom {}))

(def tcp-responce-ch (atom (chan 1000)))

(defn dispatch-to-callback [callback data]
  ;(println "CALLBACK::" callback)
  (let [data (dissoc data :request-uuid)]
    (cond (instance? MMC callback)
          (>!! callback data)
          (instance? IFn callback)
          (callback data)
          :else (throw (Exception. (str "Callback of type " (str (class callback)) " not supported"))))))

(defn start-tcp-responce-async-loop [client]
  (thread (loop []
        (let [callback (<!! @tcp-responce-ch)
              stream-in-ch (:in @client)]
          (if-not (nil? stream-in-ch)
            (let [responce (<!! stream-in-ch)
                  ;_ (println "got responce" responce)
                  responce (util/de-serialize (byte-array responce))
                  request-uuid (keyword (get responce :request-uuid))
                  callback (get @pending-requests request-uuid)]
              ;gc atom
              (swap! pending-requests dissoc request-uuid)
              (dispatch-to-callback callback responce))
            (dispatch-to-callback callback {:error ":in socket channel closed, no data received"}))
          (recur)))))

(def tcp-request-ch (atom (chan 1000)))

(defn start-tcp-request-async-loop [client]
  (thread (loop []
        (let [{:keys [callback b-a request-uuid]} (<!! @tcp-request-ch)
              request-uuid (keyword request-uuid)
              stream-out-ch (:out @client)
              send-result (if stream-out-ch
                            (>!! stream-out-ch b-a)
                            false)]
          ;add to pending-requests
          (swap! pending-requests assoc request-uuid callback)
          (if send-result
            ;schedule a "take" from the socket
            (do
              ;(println "scheduling a take...")
              (>!! @tcp-responce-ch callback))
            (dispatch-to-callback callback {:error ":out socket channel closed, no data sent"}))
          (recur)))))


(defn init
  "Initiates a tcp socket connection to the server"
  [host port]
  (reset! pending-requests {})
  (reset! tcp-request-ch (chan 1000))
  (reset! tcp-responce-ch (chan 1000))
  (reset! client (connect-stream-to-core-async-channels
                   @(aleph-netty/connect-client host port)))
  (start-tcp-request-async-loop client)
  (start-tcp-responce-async-loop client))

(defn dispatch-to-type [value]
  (util/serialize value))

(def callback-count (atom 0))

(defn callback-test [responce-data]
  ;(println "callback-test responce-data:" responce-data)
  (swap! callback-count + 1))


(defn- header-bytes
  "Bytes common to all requests"
  [command]
  (let [language (byte-array [0])
        api-version (byte-array [api-version])
        command (byte-array [command])
        verbose-mode (byte-array [0])
        uuid (util/uuid)
        request-uuid (util/uuid-to-bytes uuid)]
    {:header-bytes (byte-array (concat language api-version command verbose-mode request-uuid))
     :request-uuid (str uuid)}))


(defn- io-assoc-base [^String entity-id key value]
  (let [{:keys [header-bytes request-uuid]} (header-bytes 1)
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])
        ;1 byte for the type - nippy
        value-type-byte (byte-array [8])
        value-bytes (dispatch-to-type value)
        value-length (util/int-to-four-bytes (int (inc (count value-bytes))))]
    {:b-a (byte-array (concat header-bytes entity-id-length entity-id-bytes key-length key-bytes value-length value-type-byte value-bytes))
     :request-uuid request-uuid}))

(defn- io-dissoc-base [^String entity-id key]
  (let [{:keys [header-bytes request-uuid]} (header-bytes 2)
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])]
    {:b-a (byte-array (concat header-bytes entity-id-length entity-id-bytes key-length key-bytes))
     :request-uuid request-uuid}))

(defn- io-get-key-as-of-base [^String entity-id key timestamp]
  (let [{:keys [header-bytes request-uuid]} (header-bytes -128)
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])
        timestamp (.getBytes (str timestamp))]
    {:b-a (byte-array (concat header-bytes entity-id-length entity-id-bytes key-length key-bytes timestamp))
     :request-uuid request-uuid}))

(defn- io-get-entity-as-of-base [^String entity-id timestamp]
  (let [{:keys [header-bytes request-uuid]} (header-bytes -127)
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        timestamp (.getBytes (str timestamp))]
    {:b-a (byte-array (concat header-bytes entity-id-length entity-id-bytes timestamp))
     :request-uuid request-uuid}))

(defn- io-get-all-versions-between-base [^String entity-id timestamp-start timestamp-end limit]
  (let [{:keys [header-bytes request-uuid]} (header-bytes -126)
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        timestamp-start (.getBytes (str timestamp-start))
        timestamp-end (.getBytes (str timestamp-end))
        limit (util/int-to-four-bytes limit)]
    {:b-a (byte-array (concat header-bytes entity-id-length entity-id-bytes timestamp-start timestamp-end limit))
     :request-uuid request-uuid}))


(defn io-template [^IFn io-fn callback]
  (let [{:keys [b-a request-uuid]} (io-fn)]
    (>!! @tcp-request-ch {:b-a b-a :request-uuid request-uuid :callback callback})))

(defn io-template-sync [^IFn io-fn]
  (let [callback (chan 1)]
    (io-template io-fn callback)
    (<!! callback)))

;PUBLIC API

;writes
(defn io-assoc
  ([^String entity-id key value]
   (io-template-sync #(io-assoc-base entity-id key value)))
  ([^String entity-id key value callback]
   (io-template #(io-assoc-base entity-id key value) callback)))

(defn io-hash-map [entity-id a-map]
  (doseq [[k v] a-map]
    (io-assoc entity-id k v (fn [x] (println "confirmed write " k v x)))))

(defn io-dissoc
  ([^String entity-id key]
   (io-template-sync #(io-dissoc-base entity-id key)))
  ([^String entity-id key callback]
   (io-template #(io-dissoc-base entity-id key) callback)))

;reads
(defn io-get-key-as-of
  ([^String entity-id key timestamp]
   (io-template-sync #(io-get-key-as-of-base entity-id key timestamp)))
  ([^String entity-id key timestamp callback]
   (io-template #(io-get-key-as-of-base entity-id key timestamp) callback)))

(defn io-get-entity-as-of
  ([^String entity-id timestamp]
   (io-template-sync #(io-get-entity-as-of-base entity-id timestamp)))
  ([^String entity-id timestamp callback]
   (io-template #(io-get-entity-as-of-base entity-id timestamp) callback)))

(defn io-get-all-versions-between
  ([^String entity-id timestamp-start timestamp-end limit]
   (io-template-sync #(io-get-all-versions-between-base entity-id timestamp-start timestamp-end limit)))
  ([^String entity-id timestamp-start timestamp-end limit callback]
   (io-template #(io-get-all-versions-between-base entity-id timestamp-start timestamp-end limit) callback)))

(defn io-get-entity
  ([entity-id]
   (io-get-entity-as-of entity-id latest-server-timestamp))
  ([entity-id callback]
   (io-get-entity-as-of entity-id latest-server-timestamp callback)))

(defn io-get-key
  ([entity-id key]
   (io-get-key-as-of entity-id key latest-server-timestamp))
  ([entity-id key callback]
    (io-get-key-as-of entity-id key latest-server-timestamp callback)))
