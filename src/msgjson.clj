(ns msgjson
  (:gen-class)
  (:require [org.httpkit.server :as server])
  (:require [org.httpkit.client :as http])
  (:require [clj-json.core :as json])
  (:require [msgpack.core :as msgpack])
  (:use [clojure.java.io :only [input-stream]])
  (:use [compojure.handler :only [api]])
  (:use [compojure.core :only [defroutes GET]]))

(def useragent "msgjson")

(def timeout 30000)

(def msgpack-error
  {:status 500 :headers {"Content-Type" "application/x-msgpack"}})

(def json-error
  {:status 500 :headers {"Content-Type" "application/json"}})

(defn repack-msgpack [msg]
  (try
    (binding [json/*coercions* {Byte (fn [x] (.intValue x))}]
      {:headers {"Content-Type" "application/json"} :body (json/generate-string (msgpack/unpack msg))})
    (catch Exception e json-error)))

(defn repack-json [json]
  (try
      {:headers {"Content-Type" "application/x-msgpack"} :body (input-stream (msgpack/pack (json/parse-string json)))}
      (catch Exception e msgpack-error)))

(defn show-landing-page [request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (slurp "public/index.html")})

(defn handle-json-to-msgpack [request]
  (def url (get (get request :params) :url))
  (if (nil? url)
    (show-landing-page request)
    (server/with-channel request channel
      (def options {:timeout timeout
                    :as :text
                    :user-agent useragent})
      (http/get url options
              (fn [{:keys [status headers body error]}]
                (if error
                  (server/send! channel msgpack-error)
                  (server/send! channel (repack-json body))))))))

(defn handle-msgpack-to-json [request]
  (def url (get (get request :params) :url))
  (if (nil? url)
    (show-landing-page request)
    (server/with-channel request channel
      (def options {:timeout timeout
                    :as :byte-array
                    :user-agent useragent})
      (http/get url options
              (fn [{:keys [status headers body error]}]
                (if error
                  (server/send! channel json-error)
                  (server/send! channel (repack-msgpack body))))))))

(defroutes all-routes
  (GET "/msgpack" [] handle-json-to-msgpack)
  (GET "/json" [] handle-msgpack-to-json)
  (GET "/*" [] show-landing-page))

(defn -main [port]
  (server/run-server (api #'all-routes) {:port (Integer. port)}))
