(ns queues.logic.matcher
  (:require [clojure.spec.alpha :as s]
            [queues.logic.jobs :as logic.jobs]
            [queues.specs.agents :as specs.agents]
            [queues.specs.agents :as specs.agents]
            [queues.specs.general :as specs.general]
            [queues.specs.job :as specs.job]
            [queues.specs.queues :as specs.queues]
            [queues.specs.matching-criteria :as specs.matching-criteria]
            [queues.specs.order-of-priority :as specs.order-of-priority]))

(defn- first-job-matching-skill-and-urgency
  [jobs urgent skill]
  (-> (filter #(logic.jobs/job-matches-skill-and-urgency? % skill urgent) jobs)
      first))

(s/fdef first-job-matching-skill-and-urgency
        :args (s/cat :jobs ::specs.queues/jobs-waiting
                     :urgent ::specs.job/urgent
                     :skill ::specs.general/skill)
        :ret (s/or :job-found ::specs.job/job
                   :job-not-found nil?))

(defn- first-job-matching-agent-on-specific-criteria
  [jobs agent {skillset ::specs.matching-criteria/skillset
               urgent ::specs.matching-criteria/urgent :as matching-criteria}] ;; matching-criteria is used in fdef
  (let [skills (skillset agent)
        ; TODO [QUESTION; READ] how to get around this name repetition. How should I think about it. there is a readability vs collision risk trade-off
        first-job-matching-skill-func (partial first-job-matching-skill-and-urgency jobs urgent)]
    (some first-job-matching-skill-func skills)))

(s/fdef first-job-matching-agent-on-specific-criteria
        :args (s/cat :jobs ::specs.queues/jobs-waiting
                     :agent ::specs.agents/agent
                     :matching-criteria ::specs.matching-criteria/matching-criteria)
        :ret (s/or :job-found ::specs.job/job
                   :no-job nil?)
        :fn (s/or :job-found #(let [{skill-type ::specs.matching-criteria/skillset
                                     urgent ::specs.matching-criteria/urgent} (-> % :args :priority)
                                    {job-type ::specs.job/type
                                     job-urgent ::specs.job/urgent} (-> % :ret :job-found)
                                    agent-skill (-> % :args :agent skill-type first)]
                                (and (= urgent job-urgent)
                                     (= agent-skill job-type)))
                  :no-job #(nil? (-> % :ret :no-job))))

(defn first-job-matching-agent
  [agent jobs order-of-priority]
  (some (partial first-job-matching-agent-on-specific-criteria
                 jobs
                 agent)
        order-of-priority))

(s/fdef first-job-matching-agent
        :args (s/cat :agent ::specs.agents/agent
                     :jobs ::specs.queues/jobs-waiting
                     :order-of-priority ::specs.order-of-priority/order-of-priority)
        :ret (s/or :no-job nil?
                   :job-found ::specs.job/job))

(defn- agent-matches-job-type-through-skillset?
  [agent job-type skillset-type]
  (let [skills (skillset-type agent)
        matches? (some #(= job-type %) skills)]
    (if matches? true false)))

(s/fdef agent-matches-job-type-through-skillset?
        :args (s/cat :agent ::specs.agents/agent
                     :job-type ::specs.job/type
                     :skillset ::specs.agents/skillset-type)
        :ret boolean?)

(defn- first-agent-matching-job-type-through-skillset
  [job-type agents-waiting-for-job skillset-type]
  (first (filter #(agent-matches-job-type-through-skillset? % job-type skillset-type) agents-waiting-for-job)))

(s/fdef first-agent-matching-job-type-through-skillset
        :args (s/cat :job-type ::specs.job/type
                     :agents-waiting-for-job ::specs.queues/agents-waiting-for-job
                     :skillset-type ::specs.agents/skillset-type)
        ;;TODO [QUESTION; RUNTIME CHECK] add check for agent having the job-type in its skillsets?
        :ret (s/or :agent-found ::specs.agents/agent
                   :not-found nil?))

(defn first-agent-matching-job-type
  [job-type agents-waiting-for-job]
  (some (partial first-agent-matching-job-type-through-skillset
                 job-type
                 agents-waiting-for-job)
        ;;TODO [IMPROVE; ARCH] weired reference to a variable in specs
        specs.agents/skillset-types-in-order-of-priority))

(s/fdef first-agent-matching-job-type
        :args (s/cat :job-type ::specs.job/type
                     :agents-waiting-for-job ::specs.queues/agents-waiting-for-job)
        :ret (s/or :agent-found ::specs.agents/agent
                   :not-found nil?))