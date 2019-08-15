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
  ; Output should give a breakdown of the job queue, consisting of all completed jobs,
  ; jobs that are being done and jobs waiting to be assigned to an agent.
  #{["/"             :get  echo :route-name :queue-state]
    ; create a new agent, returns agent :id and status
    ["/agents"       :post echo :route-name :agent-create]
    ; returns how many jobs of each type this agent has performed and its status
    ["/agents/:id"   :get  echo :route-name :agent-status]
    ; create a new job, returns job :id and status
    ["/jobs"         :post echo :route-name :job-create]
    ; create a job-request, return the job-id assigned or the job-request with waiting status
    ["/job-requests" :post echo :route-name :job-request-create]})

(def service {:env :prod
              ::http/routes routes
              ::http/type :jetty
              ::http/port 8080})