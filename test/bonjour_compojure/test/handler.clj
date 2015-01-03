(ns bonjour_compojure.test.handler
  (:require [clojure.test :refer :all]
            [bonjour_compojure.handler :refer :all]
            [ring.mock.request :as mock]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (re-find #"Acme Corp" (slurp (:body response))))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))

  (testing "GET /bonjours"
    (let [response (app (mock/request :get "/bonjours"))]
      (is (= (:status response) 200))
      (is (> (count (:body response)) 0)))))
