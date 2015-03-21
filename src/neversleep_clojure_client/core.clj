(ns neversleep-clojure-client.core
  (:require [neversleep-clojure-client.aleph-netty :as aleph-netty]
            [clojure.core.async :refer [<!! >!! go <! >! alts! chan thread timeout pipe close!]]
            [clojure.core.async.impl.protocols :refer [closed?]]
            [manifold.stream :as s]
            [neversleep-clojure-client.util :as util])
  (:gen-class)
  (:import (jv SystemClock)))

(defn -main
  [& args])


(def client (atom nil))

(defn connect-stream-to-core-async-channels [s]
  (let [stream-in-ch (chan 1024)
        stream-out-ch (chan 1024)]
    (s/connect s stream-in-ch)
    (s/connect stream-out-ch s)
    {:in stream-in-ch
     :out stream-out-ch}))

(defn send-byte-array [^bytes b-a]
  (let [stream-out-ch (:out @client)]
    (if stream-out-ch
      (>!! stream-out-ch b-a)
      false)))

(defn receive-byte-array []
  (let [stream-in-ch (:in @client)]
    (<!! stream-in-ch)))

(defn init [host port]
  (reset! client (connect-stream-to-core-async-channels
                   @(aleph-netty/connect-client host port))))

(defn dispatch-to-type [value]
  (util/serialize value))

(defn tcp-request-responce [^bytes b-a]
  (send-byte-array b-a)
  (clojure.walk/keywordize-keys (cheshire.core/decode (String. (byte-array (receive-byte-array))))))

;PUBLIC API

(defn io-assoc [entity-id key value]
  (let [api-version (byte-array [1])
        command (byte-array [1])
        verbose-mode (byte-array [0])
        request-uuid (util/uuid-to-bytes (util/uuid))
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])
        ;1 byte for the type - nippy
        value-type-byte (byte-array [8])
        value-bytes (dispatch-to-type value)
        value-length (util/int-to-four-bytes (int (inc (count value-bytes))))
        b-a (byte-array (concat api-version command verbose-mode request-uuid entity-id-length entity-id-bytes key-length key-bytes value-length value-type-byte value-bytes))]
    (tcp-request-responce b-a)))

(defn io-dissoc [entity-id key]
  (let [api-version (byte-array [1])
        command (byte-array [2])
        verbose-mode (byte-array [0])
        request-uuid (util/uuid-to-bytes (util/uuid))
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])
        b-a (byte-array (concat api-version command verbose-mode request-uuid entity-id-length entity-id-bytes key-length key-bytes))]
    (tcp-request-responce b-a)))

(defn io-get-key-as-of [entity-id key timestamp]
  (let [api-version (byte-array [1])
        command (byte-array [-128])
        verbose-mode (byte-array [0])
        request-uuid (util/uuid-to-bytes (util/uuid))
        entity-id-bytes (.getBytes entity-id)
        entity-id-length (byte-array [(count entity-id-bytes)])
        key-bytes (.getBytes (name key))
        key-length (byte-array [(count key-bytes)])
        timestamp (.getBytes (str timestamp))
        b-a (byte-array (concat api-version command verbose-mode request-uuid entity-id-length entity-id-bytes key-length key-bytes timestamp))]
    (tcp-request-responce b-a)))

(defn io-get-entity-as-of [entity-id timestamp]
  (let [api-version (byte-array [1])
        command (byte-array [-127])
        verbose-mode (byte-array [0])
        request-uuid (util/uuid-to-bytes (util/uuid))
        entity-id-length (byte-array [(count entity-id)])
        entity-id-bytes (.getBytes entity-id)
        timestamp (.getBytes (str timestamp))
        b-a (byte-array (concat api-version command verbose-mode request-uuid entity-id-length entity-id-bytes timestamp))]
    (tcp-request-responce b-a)))

(defn io-get-entity-latest [entity-id]
  (io-get-entity-as-of entity-id "___________________"))
