(ns queues.core
  (:require [queues.models.events :as events]
            [queues.models.agents-and-jobs :as aajs]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.job-assigned :as ja])
  (:gen-class))

(defn agent-found
  "Receives agents-and-jobs and a job-request content and returns the agent related
  to that job request"
  [agents-and-jobs job-req-content]
  (let [agent-id ((comp first vals) job-req-content)]
    (->> agents-and-jobs
         (::aajs/agents)
         (drop-while #(not= agent-id (::agent/id %)))
         (first))))

(defn job-found
  "Receives agents-and-jobs and an agent and finds the most suitable job for that agent"
  [agents-and-jobs agent])

(defn matching-waiting-job
  "Receives agents-and-jobs and a job request and returns a matching job
  if no matching job exists returns nil"
  [agents-and-jobs job-req-content]
  (->> job-req-content
       (agent-found agents-and-jobs)
       (job-found agents-and-jobs)))

(defn queued-job-request
  "Receives agents-and-jobs and a job request content and returns
  agents-and-jobs with a job request "
  [agents-and-jobs job-req-content])

(defn update-job-assigneds-func
  "Receives a job to be assigned to an agent and returns a function that
  creates a job assigned object and conjures it to an afterwards provided
  jobs-assigned vector"
  [job job-req-content]
  (fn [jobs-assigned]
    (conj jobs-assigned {::ja/job-assigned {::job/id   (::job/id job)
                                            ::agent/id (::agent/id job-req-content)}})))

(defn assigned-job
  "Receives agents-and-jobs and a job request content and returns
  agents-and-jobs with a job assigned with that job request id"
  [agents-and-jobs job-req-content job]
  (update agents-and-jobs
          ::aajs/jobs-assigned
          (update-job-assigneds-func job job-req-content)))

(defn processed-job-req
  "Receives agents-and-jobs and a job request content and returns an agents and jobs
  with job req either queued if no agents are available or assigned if an agent is available"
  [agents-and-jobs job-req-content]
  (let [matching-job (matching-waiting-job agents-and-jobs job-req-content)]
    (if (nil? matching-job)
      (queued-job-request agents-and-jobs job-req-content)
      (assigned-job agents-and-jobs job-req-content matching-job))))

(defn added-event
  "Receives a map of agents and jobs asigned and an event
  processes the event and adds the result to agents and jobs"
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