(ns queues.service
  (:require [clojure.spec.alpha :as s]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]))

(defn response [status body headers]
  {:status status :body body :headers headers})

(s/fdef response
        :args (s/cat :status int?
                     :body string?
                     :headers string?)
        :ret map?)

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))

(def echo
  {:name :echo
   :enter
         (fn [context]
           (let [body (:body context)
                 headers (:headers context)
                 resp (ok body headers)]
             (assoc context :response resp)))})

(def job-queues
  {:name  :job-queues
   :enter (fn [context]
            ;;(->> (state/all-agents-indexed-by-id (init/db))
            ;;     (map #(first (vals %)))
            ;;     (json-converter/json-events-str-formatted-from-clj-events))
            (let [body "{\n  \"jobs_done\": [],\n  \"jobs_being_done\": [],\n  \"jobs_queued\": []\n}"
                  headers (:headers context)
                  resp (ok body headers)]
              (assoc context :response resp)))})

(def routes
  (route/expand-routes
    ; Output should give a breakdown of the job queue, consisting of all completed jobs,
    ; jobs that are being done and jobs queued to be assigned to an agent.
    #{["/" :get job-queues :route-name :job-queues]
      ; create a new agent, returns agent :id and status
      ["/agents" :post echo :route-name :agent-create]
      ; returns how many jobs of each type this agent has performed and its status
      ["/agents/:id" :get echo :route-name :agent-status]
      ; create a new job, returns job :id and status
      ["/jobs" :post echo :route-name :job-create]
      ; create a job-request, return the job-id assigned or the job-request with queued status
      ["/job-requests" :post echo :route-name :job-request-create]}))

(def service {:env          :prod
              ::http/routes routes
              ::http/type   :jetty
              ::http/port   8080})