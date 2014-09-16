(ns bonjour_compojure.database
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig app-config (io/resource "app.edn"))

(def conn (atom nil))
(def bonjourdb (atom nil))

(defn init
  "initializes the connection to the database and returns the atom representing the db object.
  Should be called at app startup."
  []
  (let [config (app-config)
        host (:mongo-host config)
        port (:mongo-port config)
        database (:database config)]
    (if (and (nil? host) (nil? port))
      (reset! conn (mg/connect))
      (reset! conn (mg/connect {:host host :port port})))
    (if (not (nil? @conn))
      (reset! bonjourdb (mg/get-db @conn database))
      (throw (RuntimeException.
             (str "The connection to" host ":" port "/" database "failed to open"))))))

(defn list-empty?
  "returns true if the bonjour collection contains any element"
  []
  (let [config (app-config)
        database (:database config)]
    (mc/any? @bonjourdb database)))

(defn find-by-date
  "finds a bonjour by date"
  [date]
  (let [config (app-config)
        collection (:bonjour config)]
    (from-db-object (mc/find @bonjourdb collection {:date date}) true)))


;; (defn collection-exists?
;;   "tests the existence of the named collection"
;;   [collection]
;;   (mc/exists? @bonjourdb collection))

;; (defn create-collection
;;   "Creates the bonjour collection. Takes the bonjour app subject as argument."
;;   [subject]
;;   (if (not (collection-exists? subject))
;;     (mc/create @bonjourdb subject)))




