(defproject com.raspasov/neversleep-clojure-client "1.0.0-alpha1"
  :description "Immutable data structure server, in Clojure (client)"
  :url "https://github.com/raspasov/neversleep-clojure-client"
  :license {:name "MIT License"}
  :java-source-paths ["src/jv"]
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [aleph "0.4.0-beta3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.taoensso/nippy "2.8.0"]
                 [environ "1.0.0"]
                 [org.clojure/core.incubator "0.1.3"]
                 [criterium "0.4.3"]
                 [gloss "0.2.4"]
                 [cheshire "5.4.0"]]
  ;:global-vars {*warn-on-reflection* true *unchecked-math* :warn-on-boxed}
  :plugins [[lein-environ "1.0.0"]]
  :main ^:skip-aot neversleep-clojure-client.core
  :target-path "target/%s"
  :jvm-opts ^:replace ["-XX:+AggressiveOpts"
                       "-XX:+UseFastAccessorMethods"
                       "-XX:+UseG1GC"
                       "-Xms512m"
                       "-Xmx512m"
                       ;"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
                       ;"-agentpath:/Users/raspasov/Downloads/YourKit_Java_Profiler_2015_EAP_build_15038.app/Contents/Resources/bin/mac/libyjpagent.jnilib"
                       ]
  :profiles {:uberjar {:aot :all}})
