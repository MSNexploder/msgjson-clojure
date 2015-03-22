(defproject msgjson "0.0.2"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.1.19"]
                 [clojure-msgpack "0.1.0-20140117.081453-5"]
                 [clj-json "0.5.3"]
                 [compojure "1.3.2"]]
  :uberjar-name "msgjson-standalone.jar"
  :main msgjson
  :min-lein-version "2.5.0")
