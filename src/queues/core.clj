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

(defn job-not-matches?
  [skillset urgent job]
  (or (not= skillset (::job/type job))
      (not= urgent (::job/urgent job))))

(defn job-with-prior
  "Receives an agent, a jobs-waiting list and a priority and returns
  the first job that matches the skill-type and urgency provided in proiority"
  [agent jobs-waiting priority]
  (let [skill-type (:skill-type priority)
        urgent (:urgent priority)
        skillset (first (skill-type agent))]
    (println skill-type "\n" urgent "\n" skillset)
    (-> (drop-while #(job-not-matches? skillset urgent %) jobs-waiting)
        (first))))

(defn job-found
  "Receives a vector of jobs waiting and an agent
  and finds the most suitable job for that agent"
  ([jobs-waiting agent]
   (let [priority-queue [{:skill-type ::agent/primary-skillset :urgent true}
                         {:skill-type ::agent/primary-skillset :urgent false}
                         {:skill-type ::agent/secondary-skillset :urgent true}
                         {:skill-type ::agent/secondary-skillset :urgent false}]]
     (job-found jobs-waiting agent priority-queue)))
  ([jobs-waiting agent priority-queue]
   (let [priority (first priority-queue)
         job (job-with-prior agent jobs-waiting priority)]
     (if (and (nil? job) (not-empty (rest priority-queue)))
       (job-found jobs-waiting agent (rest priority-queue))
       job))))

;if job-found has analysed the last priority in the priority queue
;it will return job anyway because job either has the found job in
;case the last priority had a match in jobs-waiting or job has nil
;which is the design response for when no job was found

;;TODO: include prioriry queue as part of agents and jobs map
;; this way it becomes a hard coded input but that is clear and set right in the beginning
;; of the program becoming easier to change it later

(defn matching-waiting-job
  "Receives agents-and-jobs and a job request and returns a matching job
  if no matching job exists returns nil"
  [agents-and-jobs job-req-content]
  (->> job-req-content
       (agent-found agents-and-jobs)
       (job-found (::aajs/jobs-waiting agents-and-jobs))))

(defn queued-job-request
  "Receives agents-and-jobs and a job request content and returns
  agents-and-jobs with a job request "
  [agents-and-jobs job-req-content]
  (update agents-and-jobs ::aajs/job-requests-waiting conj job-req-content))

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

(defn agent-skillsets
  "Receives an agent an returns a coll with its skillsets"
  [agent]
  (concat (::agent/primary-skillset agent)
          (::agent/secondary-skillset agent)))

(defn matching-waiting-job-req
  "Receives an 'agents-and-jobs' map and a job
  and returns a matching job request or nil if non exists"
  [agents-and-jobs job-content]
  (some (fn [job-request]
          (->> job-request
               (agent-found agents-and-jobs)
               (agent-skillsets)
               (some #{(::job/type job-content)})
               (#(if % job-request))))
        (::aajs/job-requests-waiting agents-and-jobs)))

(defn queued-job
  "Receives an 'agents-and-jobs' map and a job content
  and returns the 'agents-and-jobs' map with the job
  queued in the job jobs waiting map"
  [agents-and-jobs job]
  (update agents-and-jobs ::aajs/jobs-waiting #(conj % job)))

(defn processed-new-job
  "Receives an 'agents and jobs' map and an event content and returns
  the 'agents and jobs' either with the new job assigned, if there were
  matching waiting job requests or queed in jobs waiting otherwise"
  [agents-and-jobs job-content]
  (let [matching-job-req (matching-waiting-job-req agents-and-jobs job-content)]
    (if (nil? matching-job-req)
      (queued-job agents-and-jobs job-content)
      (assigned-job agents-and-jobs matching-job-req job-content))))

(defn added-event
  "Receives a map of agents and jobs asigned and an event
  processes the event and adds the result to agents and jobs"
  [agents-and-jobs event]
  (let [type ((comp first keys) event)
        content ((comp first vals) event)]
    (case type
      ::events/new-agent (update agents-and-jobs ::aajs/agents #(conj % content))
      ::events/new-job (processed-new-job agents-and-jobs content)
      ::events/job-request (processed-job-req agents-and-jobs content)
      agents-and-jobs)))

;;TODO: evolve added-event to be modeled as a multimethod with event type as dispatch value

;;FIXME: create new-job processing function. See bellow
;; Before including in jobs-waiting new-job processing should check if there are
;; job requests waiting that match that job. In this cases it assigns the job
;; without including in the jobs-waiting line

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

;;TODO: create functions to convert jason files from input into clojure input
;;TODO: create functions to convert clojure output into json output files

(defn -main
  [& args]
  (println "Hello, World!"))

;;TODO: implement main functions
;;TODO: implement run time type checks for variables and clojure spec fdefn for functions