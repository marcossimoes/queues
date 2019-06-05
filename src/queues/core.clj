(ns queues.core
  (:require [queues.models.events :as events])
  (:gen-class))

(defn added-event
  "Receives a map of agents and jobs asigned and an event
  processes the event and adds the result to agents and "
  [agents-and-jobs event]
  (let [type ((comp first keys) event)
        content ((comp first vals) event)]
    (case type
      ::events/new-gent (update agents-and-jobs :agents #(conj % content))
      agents-and-jobs)))

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  ([events]
   (let [agents-and-jobs {:agents []
                          :jobs-assigned []
                          :jobs-waiting []}]
     (dequeue events
              agents-and-jobs)))
  ([events agents-and-jobs]
   (->> events
        (reduce (partial added-event) agents-and-jobs)
        (:jobs-assigned))))

(defn -main
  [& args]
  (println "Hello, World!"))