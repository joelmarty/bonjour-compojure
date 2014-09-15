(ns bonjour_compojure.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [resource-response response not-found]]
            [ring.middleware.json :as middleware]
            [bonjour_compojure.controllers :as ctrl]
            [liberator.dev :refer [wrap-trace]]))


(defroutes app-routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (context "/bonjours" []
           (defroutes bonjours-routes
             (GET "/" [] ctrl/all)
             (POST "/" [] ctrl/all)
             (context "/:id" [id]
                      (defroutes bonjour-route
                        (GET "/" [] (ctrl/one id))
                        (PUT "/" [] (ctrl/one id))
                        (DELETE "/" [] (ctrl/one id))
                        ))))
  (route/resources "/")
  (route/not-found (not-found "Not Found")))

(def app
  (-> (handler/api app-routes)
;;       (middleware/wrap-json-body)
      (middleware/wrap-json-response {:pretty true})
      (middleware/wrap-json-params)
      (wrap-trace :ui)))
