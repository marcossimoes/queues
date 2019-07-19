(ns queues.core
  (:require [queues.models.specs :as specs]
            [queues.json :as json]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s])
  (:gen-class))

(def ^:dynamic *logging* false)

(defn agent-found
  "Receives agents-and-jobs and a job-request content and returns the agent related
  to that job request or nil if no agent is found"
  [agents-and-jobs job-req-content]
  (let [agent-id ((comp first vals) job-req-content)]
    (->> agents-and-jobs
         (::specs/agents)
         (drop-while #(not= agent-id (::specs/agent.id %)))
         (first))))

(s/fdef agent-found
        :args (s/cat :agents-and-jobs ::specs/agents-and-jobs
                     :job-req-content ::specs/job-req)
        :ret (s/or :found ::specs/agent
                   :not-found nil?)
        :fn (s/or :found #(= (-> % :ret :found ::specs/agent.id)
                             (-> % :args :job-req-content ::specs/job-req.agent-id))
                  :not-found #(nil? (-> :ret :not-found %))))

(defn job-not-matches?
  [skillset urgent job]
  (or (not= skillset (::specs/job.type job))
      (not= urgent (::specs/job.urgent job))))

(s/fdef job-not-matches?
        :args (s/cat :skillset (s/or :has-skillset ::specs/skill
                                     :no-skillset nil?)
                     :urgent ::specs/job.urgent
                     :job ::specs/job)
        :ret boolean?)

(defn job-with-prior
  "Receives an agent, a jobs-waiting list and a priority and returns
  the first job that matches the skill-type and urgency provided in priority
  or nil if no job is found"
  [agent jobs-waiting priority]
  (let [skill-type (::specs/priority.skill-type priority)
        urgent (::specs/priority.urgent priority)
        skillset (first
                   (skill-type agent))]
    (-> (drop-while #(job-not-matches? skillset urgent %) jobs-waiting)
        (first))))

(s/fdef job-with-prior
        :args (s/cat :agent ::specs/agent
                     :jobs-waiting ::specs/jobs-waiting
                     :priority ::specs/priority)
        :ret (s/or :job-found ::specs/job
                   :no-job nil?)
        :fn (s/or :job-found #(let [{skill-type ::specs/priority.skill-type
                                     urgent ::specs/priority.urgent} (-> % :args :priority)
                                    {job-type ::specs/job.type
                                     job-urgent ::specs/job.urgent} (-> % :ret :job-found)
                                    agent-skill (-> % :args :agent skill-type first)]
                                (and (= urgent job-urgent) (= agent-skill job-type)))
                  :no-job #(nil? (-> % :ret :no-job))))

(defn job-found
  "Receives a vector of jobs waiting and an agent
  and finds the most suitable job for that agent"
  ([jobs-waiting agent]
   (let [priority-queue [{::specs/priority.skill-type ::specs/agent.primary-skillset ::specs/priority.urgent true}
                         {::specs/priority.skill-type ::specs/agent.primary-skillset ::specs/priority.urgent false}
                         {::specs/priority.skill-type ::specs/agent.secondary-skillset ::specs/priority.urgent true}
                         {::specs/priority.skill-type ::specs/agent.secondary-skillset ::specs/priority.urgent false}]]
     (job-found jobs-waiting agent priority-queue)))
  ([jobs-waiting agent priority-queue]
   (let [priority (first priority-queue)
         job (job-with-prior agent jobs-waiting priority)]
     (if (and (nil? job) (not-empty (rest priority-queue)))
       (job-found jobs-waiting agent (rest priority-queue))
       job))))

(s/fdef job-found
        :args (s/alt :no-prior-queue (s/cat :jobs-waiting ::specs/jobs-waiting
                                            :agent ::specs/agent)
                     :with-prior-queue (s/cat :jobs-waiting ::specs/jobs-waiting
                                              :agent ::specs/agent
                                              :priority-queue ::specs/priority-queue))
        :ret (s/or :no-job nil?
                   :job-found ::specs/job))

;;TODO: include prioriry queue as part of agents and jobs map
;; this way it becomes a hard coded input but that is clear and set right in the beginning
;; of the program becoming easier to change it later

(defn agent-skillsets
  "Receives an agent an returns a coll with its skillsets"
  [agent]
  (concat (::specs/agent.primary-skillset agent)
          (::specs/agent.secondary-skillset agent)))

(defn matching-waiting-job
  "Receives agents-and-jobs and a job request and returns a matching job
  if no matching job exists returns nil"
  [agents-and-jobs job-req-content]
  (if-let [agent (agent-found agents-and-jobs job-req-content)]
    (job-found (::specs/jobs-waiting agents-and-jobs) agent)))

(s/fdef matching-waiting-job
        :args (s/cat :agents-and-jobs ::specs/agents-and-jobs
                     :job-req-content ::specs/job-req)
        :ret (s/or :no-job nil?
                   :job-found ::specs/job))

(defn matching-waiting-job-req
  "Receives an 'agents-and-jobs' map and a job
  and returns a matching job request or nil if non exists"
  [agents-and-jobs job-content]
  (some (fn [job-request]
          (->> job-request
               (agent-found agents-and-jobs)
               (agent-skillsets)
               (some #{(::specs/job.type job-content)})
               (#(if % job-request))))
        (::specs/job-requests-waiting agents-and-jobs)))

(defn queued-job
  "Receives an 'agents-and-jobs' map and a job content
  and returns the 'agents-and-jobs' map with the job
  queued in the job jobs waiting map"
  [agents-and-jobs job]
  (update agents-and-jobs ::specs/jobs-waiting #(conj % job)))

(defn queued-job-request
  "Receives agents-and-jobs and a job request content and returns
  agents-and-jobs with a job request "
  [agents-and-jobs job-req-content]
  (update agents-and-jobs ::specs/job-requests-waiting conj job-req-content))

(defn update-job-assigneds-func
  "Receives a job to be assigned to an agent and returns a function that
  creates a job assigned object and conjures it to an afterwards provided
  jobs-assigned vector"
  [job job-req-content]
  ;;(println "job-assigned: job-id=" (::specs/job.id job) " agent-id=" (::specs/job-req.agent-id job-req-content))
  (fn [jobs-assigned]
    (conj jobs-assigned {::specs/job-assigned {::specs/job-assigned.job-id   (::specs/job.id job)
                                               ::specs/job-assigned.agent-id (::specs/job-req.agent-id job-req-content)}})))

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
  ;;(println "job-assigned: job-id=" (::specs/job.id job) " agent-id=" (::specs/job-req.agent-id job-req-content))
  (-> agents-and-jobs
      (update ::specs/jobs-assigned (update-job-assigneds-func job job-req-content))
      (update ::specs/jobs-waiting (id-removed-from-vector (::specs/job.id job) ::specs/job.id))
      (update ::specs/job-requests-waiting (id-removed-from-vector (::specs/job-req.agent-id job-req-content) ::specs/job-req.agent-id))))

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
    (if (nil? matching-job)
      (queued-job-request agents-and-jobs job-req-content)
      (assigned-job agents-and-jobs job-req-content matching-job))))

(defn added-event
  "Processes a new event and inserts the result in agents and jobs map"
  [agents-and-jobs event]
  (let [[type content] (first event)]
    (case type
      ::specs/new-agent (update agents-and-jobs ::specs/agents conj content)
      ::specs/new-job (processed-new-job agents-and-jobs content)
      ::specs/job-request (processed-job-req agents-and-jobs content))))

(defn added-event-with-log
  [agents-and-jobs event]
  (let [res-aajs (added-event agents-and-jobs event)]
    (if *logging*
      (do (log/info "adding event: " event)
          (log/spyf :info "resulting aajs: %s" res-aajs))
      res-aajs)))

;;FIXME: switch implementation to case
;;FIXME: when new-agent is entered check for corresponding job-req and new-jobs
;;FIXME: when an agent id or job id is entered for the second type, update the original agent/id

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  ([events]
   (let [agents-and-jobs {::specs/agents []
                          ::specs/jobs-assigned []
                          ::specs/jobs-waiting []
                          ::specs/job-requests-waiting []}]
     (dequeue events agents-and-jobs)))
  ([events agents-and-jobs]
   (let [final-agents-and-jobs (reduce added-event-with-log agents-and-jobs events)]
     (::specs/jobs-assigned final-agents-and-jobs))))

;; TODO: implement agents as a map of agents ids and agents as values

;;FIXME: não precisa dos dois casos de arity. Não tem recorrência em dequeu

(defn processed-args
  "Receives an args vector with different strings corresponding to different run options
  and parses this options returning a map containing the configured options accordingly to the
  args vector"
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

;;TODO: research encapsulamento

;;TODO: implement the alternative of multiple skills in the primary or secondary skillset vectors