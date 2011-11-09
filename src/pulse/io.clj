(ns pulse.io
  (:require [pulse.util :as util]
            [pulse.log :as log]
            [pulse.queue :as queue]
            [clj-json.core :as json]
            [clj-redis.client :as redis])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io InputStreamReader BufferedReader PrintWriter)
           (java.net Socket SocketException ConnectException)))

(defn log [& data]
  (apply log/log :ns "io" data))

(defn bleeder [aorta-url handler]
  (let [{:keys [^String host ^Integer port auth]} (util/url-parse aorta-url)]
    (loop []
      (log :fn "bleeder" :at "connect" :aorta_host host)
      (try
        (with-open [socket (Socket. host port)
                    in     (-> (.getInputStream socket) (InputStreamReader.) (BufferedReader.))
                    out    (-> (.getOutputStream socket) (PrintWriter.))]
          (.println out auth)
          (.flush out)
          (loop []
            (when-let [line (.readLine in)]
              (handler line)
              (recur))))
        (log/log :fn "bleeder" :at "eof" :aorta_host host)
        (catch ConnectException e
          (log/log :fn "bleeder" :at "connect_exception" :aorta_host host))
        (catch SocketException e
          (log/log :fn "bleeder" :at "socket_exception" :aorta_host host)))
      (Thread/sleep 100)
      (recur))))

(defn init-bleeders [aorta-urls apply-queue]
  (log :fn "init-bleeders" :at "start")
  (doseq [aorta-url aorta-urls]
    (let [{aorta-host :host} (util/url-parse aorta-url)]
      (log :fn "init-bleeder" :at "spawn" :aorta_host aorta-host)
      (util/spawn (fn []
        (bleeder aorta-url (fn [line]
          (queue/offer apply-queue [aorta-host line]))))))))

(defn init-publishers [publish-queue redis-url chan workers]
  (let [redis (redis/init {:url redis-url})]
    (log :fn "init-publishers" :at "start" :chan chan)
    (dotimes [i workers]
      (log :fn "init-publishers" :at "spawn" :chan chan :index i)
      (util/spawn-loop (fn []
        (let [data (queue/take publish-queue)
              data-str (try
                         (json/generate-string data)
                         (catch Exception e
                           (log :fn "init-publishers" :at "exception" :data (pr-str data))
                           (throw e)))]
          (redis/publish redis chan data-str)))))))

(defn init-subscriber [redis-url chan apply-queue]
  (let [redis (redis/init {:url redis-url})]
    (log :fn "init-subscriber" :at "start" :chan chan)
    (redis/subscribe redis [chan]
      (fn [_ data-json]
        (queue/offer apply-queue (json/parse-string data-json))))))
