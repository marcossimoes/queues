(ns queues.service
  (:require [ring.util.response :as ring-resp]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            ))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok       (partial response 200))
(def created  (partial response 201))
(def accepted (partial response 202))

(def echo
  {:name :echo
   :enter
         (fn [context]
           (let [req (:request context)
                 resp (ok context)]
             (assoc context :response resp)))})

(def routes
  #{["/"             :get  echo :route-name :queue-state]
    ["/agents"       :post echo :route-name :agent-create]
    ["/agents/:id"   :get  echo :route-name :agent-status]
    ["/jobs"         :post echo :route-name :job-create]
    ["/job-requests" :post echo :route-name :job-request-create]})

(def service {:env :prod
              ::http/routes routes
              ::http/type :jetty
              ::http/port 8080})