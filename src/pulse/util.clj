(ns pulse.util
  (:import (java.util.concurrent Executors TimeUnit)))

(set! *warn-on-reflection* true)

(defn spawn [f]
  (let [t (Thread. ^Runnable f)]
    (.start t)
    t))

(defn spawn-loop [f]
  (spawn
    (fn []
      (loop []
        (f)
        (recur)))))

(defn spawn-tick [t f]
  (let [e (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleAtFixedRate e ^Runnable f 0 t TimeUnit/MILLISECONDS)))

(defn log [fmt & args]
  (locking *out*
    (apply printf (str fmt "\n") args)
    (flush)))

(defn update [m k f]
  (assoc m k (f (get m k))))

(defn re-match? [re s]
  (let [m (re-matcher re s)]
    (.find m)))
