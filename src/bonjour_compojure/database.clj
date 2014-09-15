(ns bonjour_compojure.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]
            [liberator.core :refer [log!]]))
(defconfig app-config (io/resource "app.edn"))

(def conn (atom nil))
(def bonjourdb (atom nil))

(defn init []
  (let [config (app-config)
        host (:mongo-host config)
        port (:mongo-port config)
        database (:bonjour config)]
    (log! (str "connecting to mongodb at " host ":" port))
    (if (and (nil? host) (nil? port))
      (reset! conn (mg/connect))
      (reset! conn (mg/connect {:host host :port port})))
    (reset! bonjourdb (mg/get-db @conn database))))

(defn collection-exists? [collection]
  (mc/exists? @bonjourdb collection))




