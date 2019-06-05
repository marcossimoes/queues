(ns queues.core
  (:require [queues.models.events :as events]
            [queues.models.agents-and-jobs :as aajs])
  (:gen-class))

(defn processed-job-req
  "Receives agents-and-jobs and a job request content and returns an agents and jobs
  with job req either queued if no agents are available or assigned if an agent is available"
  [agents-and-jobs job-req-content])

(defn added-event
  "Receives a map of agents and jobs asigned and an event
  processes the event and adds the result to agents and "
  [agents-and-jobs event]
  (let [type ((comp first keys) event)
        content ((comp first vals) event)]
    (case type
      ::events/new-agent (update agents-and-jobs ::aajs/agents #(conj % content))
      ::events/new-job (update agents-and-jobs ::aajs/jobs-waiting #(conj % content))
      ::events/job-request (processed-job-req agents-and-jobs content)
      agents-and-jobs)))

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  ([events]
   (let [agents-and-jobs {::aajs/agents []
                          ::aajs/jobs-assigned []
                          ::aajs/jobs-waiting []}]
     (dequeue events
              agents-and-jobs)))
  ([events agents-and-jobs]
   (->> events
        (reduce (partial added-event) agents-and-jobs)
        (::aajs/jobs-assigned))))

(defn -main
  [& args]
  (println "Hello, World!"))