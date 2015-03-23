(ns neversleep-clojure-client.aleph-netty
  (:require [manifold.deferred :as d]
            [manifold.stream :as s]
            [aleph.tcp :as tcp]
            [gloss.core :as gloss]
            [clojure.core.async :refer [<!! >!! go <! >! alts! chan thread timeout pipe close!]]
            [clojure.core.async.impl.protocols :refer [closed?]]
            [gloss.io :as io]))


(def protocol-client
  (gloss/compile-frame
    (gloss/finite-frame :int32
                        (gloss/repeated :byte :prefix :none))
    (fn [x] x)
    (fn [x] x)))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
      (s/map #(io/encode protocol %) out)
      s)
    (s/splice
      out
      (io/decode-stream s protocol))))

(defn connect-client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
           #(wrap-duplex-stream protocol-client %)))
