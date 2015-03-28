(ns neversleep-clojure-client.core-test
  (:require [clojure.test :refer :all]
            [neversleep-clojure-client.time :as time]
            [neversleep-clojure-client.util :as util]
            [clojure.core.async :refer [<!! >!! go <! >! alts! chan thread timeout pipe close!]]
            [neversleep-clojure-client.core :refer :all])
  (:import (clojure.lang APersistentMap APersistentVector)))


(defn init-tcp-connection []
  ;(init "localhost" 10000)
  (init "ec2-52-11-233-130.us-west-2.compute.amazonaws.com" 10000)
  )

(deftest io-assoc-test-1
  (init-tcp-connection)
  (let [responce (io-assoc "neversleep-clojure-client" "k-1" "v-1")]
    (is (instance? APersistentMap responce))
    (is (contains? responce :timestamp))))

(deftest io-dissoc-test-1
  (init-tcp-connection)
  (let [responce (io-dissoc "neversleep-clojure-client" "k-1")]
    (is (instance? APersistentMap responce))
    (is (contains? responce :timestamp))))

(deftest io-get-key-test-1
  (init-tcp-connection)
  (io-assoc "neversleep-clojure-client" "k-2" "v-2")
  (let [responce (io-get-key "neversleep-clojure-client" "k-2")]
    (is (instance? APersistentMap responce))
    (is (= "v-2" (:result responce)))))

(deftest io-get-entity-test-1
  (init-tcp-connection)
  (io-assoc "neversleep-clojure-client-1" "k-1" "v-1")
  (io-assoc "neversleep-clojure-client-1" "k-2" "v-2")
  (let [responce (io-get-entity "neversleep-clojure-client-1")]
    (is (instance? APersistentMap responce))
    (is (= {:k-1 "v-1" :k-2 "v-2"} (:result responce)))))

(deftest io-get-all-versions-between-test-1
  (init-tcp-connection)
  (io-assoc "neversleep-clojure-client-2" "k-1" "v-1")
  (io-assoc "neversleep-clojure-client-2" "k-2" "v-2")
  (let [responce (io-get-all-versions-between "neversleep-clojure-client-2" latest-server-timestamp end-of-times 1000)]
    (is (instance? APersistentMap responce))
    (is (instance? APersistentVector (:result responce)))
    (is (= 2 (count (:result responce))))))