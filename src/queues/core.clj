(ns queues.core
  (:require [queues.models.events :as events]
            [queues.models.agents-and-jobs :as aajs]
            [queues.models.agent :as agent]
            [queues.models.job-request :as jr]
            [queues.models.job :as job]
            [queues.models.job-assigned :as ja]
            [queues.json :as json]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:gen-class))

;;FIXME: merge all models in one single spec file

(def ^:dynamic *logging* false)

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

(defn agent-skillsets
  "Receives an agent an returns a coll with its skillsets"
  [agent]
  (concat (::agent/primary-skillset agent)
          (::agent/secondary-skillset agent)))

(defn matching-waiting-job
  "Receives agents-and-jobs and a job request and returns a matching job
  if no matching job exists returns nil"
  [agents-and-jobs job-req-content]
  (->> job-req-content
       (agent-found agents-and-jobs)
       (job-found (::aajs/jobs-waiting agents-and-jobs))))

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
  ;;(println "job-assigned: job-id=" (::job/id job) " agent-id=" (::jr/agent-id job-req-content))
  (fn [jobs-assigned]
    (conj jobs-assigned {::ja/job-assigned {::job/id   (::job/id job)
                                            ::jr/agent-id (::jr/agent-id job-req-content)}})))

(defn id-removed-from-vector
  "Receives a job-id and returns a function that takes a vector containing jobs with ids
  and returns a new vector with all the elements of the original vector but the map
  with the id provided to build the function"
  [id id-type]
  (fn [org-vector]
    (reduce (fn [new-vector m]
              (if (= (id-type m) id)
                new-vector
                (conj new-vector m)))
            []
            org-vector)))

(defn assigned-job
  "Receives agents-and-jobs and a job request content and returns
  agents-and-jobs with a job assigned with that job request id and
  that job removed from job-waiting"
  [agents-and-jobs job-req-content job]
  ;;(println "job-assigned: job-id=" (::job/id job) " agent-id=" (::jr/agent-id job-req-content))
  (-> agents-and-jobs
      (update ::aajs/jobs-assigned (update-job-assigneds-func job job-req-content))
      (update ::aajs/jobs-waiting (id-removed-from-vector (::job/id job) ::job/id))
      (update ::aajs/job-requests-waiting (id-removed-from-vector (::jr/agent-id job-req-content) ::jr/agent-id))))

(defn processed-new-job
  "Receives an 'agents and jobs' map and an event content and returns
  the 'agents and jobs' either with the new job assigned, if there were
  matching waiting job requests or queed in jobs waiting otherwise"
  [agents-and-jobs job-content]
  (let [matching-job-req (matching-waiting-job-req agents-and-jobs job-content)]
    (if (nil? matching-job-req)
      (queued-job agents-and-jobs job-content)
      (assigned-job agents-and-jobs matching-job-req job-content))))

(defn processed-job-req
  "Receives 'agents-and-jobs' and a 'job request content' and returns an 'agents and jobs'
  with 'job req' either queued if no jobs are available or assigned if a job is available"
  [agents-and-jobs job-req-content]
  (let [matching-job (matching-waiting-job agents-and-jobs job-req-content)]
    ;;(println "job-assigned: job-id=" matching-job " agent-id=" (::jr/agent-id job-req-content))
    (if (nil? matching-job)
      (queued-job-request agents-and-jobs job-req-content)
      (assigned-job agents-and-jobs job-req-content matching-job))))

(defmulti added-event (fn [_ event] ((comp first keys) event)))

(defmethod added-event ::events/new-agent [agents-and-jobs event]
  (let [res-aajs (->> event
                      ((comp first vals))
                      (update agents-and-jobs ::aajs/agents conj))]
    (if *logging*
      (do (log/info "adding event: " event)
          (log/spyf :info "resulting aajs: %s" res-aajs))
      res-aajs)))

(defmethod added-event ::events/new-job [agents-and-jobs event]
  (let [res-aajs (->> event
                      ((comp first vals))
                      (processed-new-job agents-and-jobs))]
    (if *logging*
      (do (log/info "adding event: " event)
          (log/spyf :info "resulting aajs: %s" res-aajs))
      res-aajs)))

(defmethod added-event ::events/job-request [agents-and-jobs event]
  (let [res-aajs (->> event
                      ((comp first vals))
                      (processed-job-req agents-and-jobs))]
    (if *logging*
      (do (log/info "adding event: " event)
          (log/spyf :info "resulting aajs: %s" res-aajs))
      res-aajs)))

;;FIXME: when new-agent is entered check for corresponding job-req and new-jobs
;;FIXME: when an agent id or job id is entered for the second type, update the original agent/id

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  ([events]
   (let [agents-and-jobs {::aajs/agents []
                          ::aajs/jobs-assigned []
                          ::aajs/jobs-waiting []
                          ::aajs/job-requests-waiting []}]
     (dequeue events agents-and-jobs)))
  ([events agents-and-jobs]
   (let [final-agents-and-jobs (reduce added-event agents-and-jobs events)]
     (::aajs/jobs-assigned final-agents-and-jobs))))

(defn processed-args
  ([args]
   (let [default-input {:input-file   "resources/sample-input.json.txt"
                        :log          false
                        :pretty-print false
                        :output-file  "jobs-assigned.json.txt"}]
     (processed-args args default-input)))
  ([rem-args processed-input]
   (cond
     (contains? #{"-l" "--log"} (first rem-args)) (processed-args (rest rem-args) (assoc processed-input :log true))
     (contains? #{"-p" "--pretty-print"} (first rem-args)) (processed-args (rest rem-args) (assoc processed-input :pretty-print true))
     (contains? #{"-f" "--output-file"} (first rem-args)) (processed-args (drop 2 rem-args) (assoc processed-input :output-file (second rem-args)))
     (empty? rem-args) processed-input
     :else (assoc processed-input :input-file (first rem-args)))))

(defn -main
  [& args]
  (let [{input-file   :input-file
         log          :log
         pretty-print :pretty-print
         output-file  :output-file} (processed-args args)
        output (binding [*logging* log]
                 (-> input-file
                     (slurp)
                     (json/read-json-events)
                     (dequeue)))]
    (if pretty-print (pp/pprint output))
    (->> output
         (json/write-json-events)
         (spit output-file))))

;;TODO: implement run time type checks for variables and clojure spec fdefn for functions
;;TODO: refactor file reading to use buffer and edn
;;TODO: include time stamp in the beginning of output file name so if you run the program multiple times it does not overrides the previous output file
;;TODO: centralize harcoded data like default input and output file names in a config file