(ns neversleep-clojure-client.util
  (:require [taoensso.nippy :as nippy])
  (:import (java.nio ByteBuffer)
           (java.util UUID)))


;blob partitioning
(defn int-to-four-bytes [i]
  (-> (ByteBuffer/allocate 4)
      (.putInt (int i))
      (.array)))

(defn uuid [] (UUID/randomUUID))

(defn uuid-to-bytes
  "Generates a new UUID and converts it to byte array (16 bytes long)"
  [uuid]
  (-> (ByteBuffer/allocate 16)
      (.putLong (.getMostSignificantBits uuid))
      (.putLong (.getLeastSignificantBits uuid))
      (.array)))

(defn bytes-to-uuid [^bytes b-a]
  (let [byte-buffer (ByteBuffer/wrap b-a)
        most-significant-bits (-> byte-buffer
                                  (.getLong 0))
        least-significant-bits (-> byte-buffer
                                   (.getLong 8))]
   (new UUID most-significant-bits least-significant-bits)))


;serialize/de-serialize
(defn serialize ^bytes [data]
  (let [frozen-data (nippy/freeze data {:skip-header? true :compressor nil :encryptor nil})]
    frozen-data))

(defn de-serialize [blob]
  (if blob
    (nippy/thaw blob {:skip-header? true :compressor nil :encryptor nil})
    nil))