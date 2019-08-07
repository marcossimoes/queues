(ns queues.logic.jobs
  (:require [clojure.spec.alpha :as s]
            [queues.logic.agents :as agents]
            [queues.specs.agent :as specs.agent]
            [queues.specs.general :as specs.general]
            [queues.specs.job :as specs.job]
            [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.priority-rule :as specs.priority-rule]))

(defn job-not-matches?
  [skillset urgent job]
  (or (not= skillset (::specs.job/type job))
      (not= urgent (::specs.job/urgent job))))

;;(s/fdef job-not-matches?
;;        :args (s/cat :skillset (s/or :has-skillset ::specs.general/skill
;;                                     :no-skillset nil?)
;;                     :urgent ::specs.job/urgent
;;                     :job ::specs.job/job)
;;        :ret boolean?)

(defn job-with-prior
  "Receives an agent, a jobs-waiting list and a priority and returns
  the first job that matches the skill-type and urgency provided in priority
  or nil if no job is found"
  [agent jobs-waiting priority]
  (let [skill-type (::specs.priority-rule/skill-type priority)
        urgent (::specs.priority-rule/urgent priority)
        skillset (first
                   (skill-type agent))]
    (-> (drop-while #(job-not-matches? skillset urgent %) @jobs-waiting)
        (first))))

;;(s/fdef job-with-prior
;;        :args (s/cat :agent ::specs.agent/agent
;;                     :jobs-waiting ::specs.job-queues/jobs-waiting
;;                     :priority ::specs.priority-rule/priority)
;;        :ret (s/or :job-found ::specs.job/job
;;                   :no-job nil?)
;;        :fn (s/or :job-found #(let [{skill-type ::specs.priority-rule/skill-type
;;                                     urgent ::specs.priority-rule/urgent} (-> % :args :priority)
;;                                    {job-type ::specs.job/type
;;                                     job-urgent ::specs.job/urgent} (-> % :ret :job-found)
;;                                    agent-skill (-> % :args :agent skill-type first)]
;;                                (and (= urgent job-urgent) (= agent-skill job-type)))
;;                  :no-job #(nil? (-> % :ret :no-job))))

(defn job-found
  "Receives a vector of jobs waiting and an agent
  and finds the most suitable job for that agent"
  [jobs-waiting agent priority-queue]
  (let [priority (first priority-queue)
        job (job-with-prior agent jobs-waiting priority)]
    (if (and (nil? job) (not-empty (rest priority-queue)))
      (job-found jobs-waiting agent (rest priority-queue))
      job)))

;;(s/fdef job-found
;;        :args (s/alt :no-prior-queue (s/cat :jobs-waiting ::specs.job-queues/jobs-waiting
;;                                            :agent ::specs.agent/agent)
;;                     :with-prior-queue (s/cat :jobs-waiting ::specs.job-queues/jobs-waiting
;;                                              :agent ::specs.agent/agent
;;                                              :priority-queue ::specs.job-queues/priority-queue))
;;        :ret (s/or :no-job nil?
;;                   :job-found ::specs.job/job))

;;TODO: include prioriry queue as part of agents and jobs map
;; this way it becomes a hard coded input but that is clear and set right in the beginning
;; of the program becoming easier to change it later

(defn matching-waiting-job
  "Receives job-queues and a job request and returns a matching job
  if no matching job exists returns nil"
  [job-queues job-req-content]
  (if-let [agent (agents/agent-found job-queues job-req-content)]
    (job-found (::specs.job-queues/jobs-waiting job-queues)
               agent
               (::specs.job-queues/priority-rules job-queues))))

;;(s/fdef matching-waiting-job
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-content ::specs.job-request/job-req)
;;        :ret (s/or :no-job nil?
;;                   :job-found ::specs.job/job))

(defn queued-job
  "Receives an 'job-queues' map and a job content
  and returns the 'job-queues' map with the job
  queued in the job jobs waiting map"
  [job-queues job]
  (-> job-queues
      ::specs.job-queues/jobs-waiting
      (alter conj job))
  (assoc job ::specs.job/status "waiting"))

;;(s/fdef queued-job
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-content ::specs.job/job)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/and #(= (-> % :ret ::specs.job-queues/jobs-waiting drop-last)
;;                       (-> % :args :job-queues ::specs.job-queues/jobs-waiting))
;;                   #(= (-> % :ret ::specs.job-queues/jobs-waiting last)
;;                       (-> % :args :job))))