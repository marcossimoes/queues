(ns queues.service
  (:require [clojure.spec.alpha :as s]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [queues.controllers.new-event-processor :as controllers.new-event-processor]
            [queues.init :as init]
            [queues.json-converter :as json-converter]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.events :as specs.events]
            [queues.specs.json-events :as specs.json-events]
            [queues.specs.queues :as specs.queues]
            [queues.state :as state]))

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
(def bad-request (partial response 400))

(defn create-and-return-agent-json-str-from-json-params
  [db new-agent-json-event]
  (->> new-agent-json-event
       (json-converter/clj-event-from-json-event)
       (controllers.new-event-processor/processed-event-with-log! db)
       (json-converter/json-events-str-from-clj-events)))

(s/fdef create-and-return-agent-json-str-from-json-params
        :args (s/cat :db ::specs.db/db
                     :new-agent-json-event ::specs.json-events/json-event)
        :ret string?)

;; TODO [IMPROVE] catch should return 400 only when app is sure the problem is really with the input
(defn create-agent-resp
  [db json-params headers]
  (try
    (created (create-and-return-agent-json-str-from-json-params db json-params)
             headers)
    (catch Exception e
      (bad-request (->> e (.getMessage) (str "caught exception: "))
                   headers))))

(s/fdef create-agent-resp
        :args (s/cat :db ::specs.db/db
                     :json-params (s/or :valid-json-event ::specs.json-events/json-event
                                        :no-valid-json-event nil?)
                     :headers map?)
        ;; TODO: specs responses
        :ret map?)

(def create-agent
  {:name  :create-agent
   :leave (fn [context]
            (let [json-params (get-in context [:request :json-params])
                  headers (get-in context [:request :headers])
                  resp (create-agent-resp init/*service-db* json-params headers)]
              (assoc context :response resp)))})

(def job-queues
  {:name  :job-queues
   :enter (fn [context]
            (let [jobs-done (state/all-jobs-done init/*service-db*)
                  jobs-being-done (state/all-jobs-in-progress init/*service-db*)
                  jobs-queued (state/all-jobs-waiting init/*service-db*)
                  ;; TODO [improve] understand how queues clj spec should talk to queues json spec and how conversion should be handled
                  queues {:jobs-done       jobs-done
                          :jobs-being-done jobs-being-done
                          :jobs-queued     jobs-queued}
                  resp-body (json-converter/json-events-str-from-clj-events queues)
                  headers (get-in context [:request :headers])
                  resp (ok resp-body headers)]
              (assoc context :response resp)))})

(def routes
  (route/expand-routes
    ; Output should give a breakdown of the job queue, consisting of all completed jobs,
    ; jobs that are being done and jobs queued to be assigned to an agent.
    #{["/" :get job-queues :route-name :job-queues]
      ; create a new agent, returns the agent
      ["/agents" :post [(body-params/body-params) create-agent] :route-name :agent-create]
      ;;; returns how many jobs of each type this agent has performed and its status
      ;;["/agents/:id" :get echo :route-name :agent-status]
      ;;; create a new job, returns job :id and status
      ;;["/jobs" :post echo :route-name :job-create]
      ;;; create a job-request, return the job-id assigned or the job-request with queued status
      ;;["/job-requests" :post echo :route-name :job-request-create]
      }))

(def service {:env          :prod
              ::http/routes routes
              ::http/type   :jetty
              ::http/port   8080})