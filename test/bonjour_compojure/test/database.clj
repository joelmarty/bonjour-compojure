(ns bonjour_compojure.test.database
  (:require [clojure.test :refer :all]
            [bonjour_compojure.database :as db]
            [monger.core :as mg]
            [monger.collection :as mc]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig app-config (io/resource "app.edn"))

(defn setup []
  (let [config (app-config)
        host (:mongo-host config)
        port (:mongo-port config)
        database "test"
        conn (mg/connect {:host host :port port})]
    (mg/drop-db conn "test")
    (mg/disconnect)))

(deftest test-db
  (use-fixtures :once setup)

  (testing "database connection"
    (let [db (db/init)]
      (is (instance? com.mongodb.DBApiLayer db))))

;;   (testing "fetch the bonjour collection"
;;     (let [collections (db/collections)]
;;       (is (< 0 (count collections)))))
  )
