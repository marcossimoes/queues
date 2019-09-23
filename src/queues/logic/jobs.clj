(ns queues.logic.jobs
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.general :as specs.general]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-req]))

(defn job-matches-skill-and-urgency?
  [job skill urgent]
  (and (= skill (::specs.job/type job))
       (= urgent (::specs.job/urgent job))))

(s/fdef job-matches-skill-and-urgency?
        :args (s/cat :job ::specs.job/job
                     :skill ::specs.general/skill
                     :urgent ::specs.job/urgent)
        :ret boolean?)

(defn- job-assigned-map-from-agent-and-job-ids [agent-id job-id]
  #::specs.job-assigned{:job-assigned #::specs.job-assigned{:job-id   job-id
                                                            :agent-id agent-id}})

(s/fdef job-assigned-map-from-agent-and-job-ids
        :args (s/cat :agent-id ::specs.agents/id
                     :job-id ::specs.job/id)
        :ret ::specs.job-assigned/job-assigned-map)

(defn job-assigned-map-from-job-with-assigned-agent
  [job-with-assigned-agent]
  (let [agent-id (::specs.job/assigned-agent job-with-assigned-agent)
        job-id (::specs.job/id job-with-assigned-agent)]
    (job-assigned-map-from-agent-and-job-ids agent-id job-id)))

(s/fdef job-assigned-map-from-job-with-assigned-agent
        :args (s/cat :job-with-assigned-agent ::specs.job/job)
        :ret ::specs.job-assigned/job-assigned)

(defn job-with-agent-assigned
  [agent-id job]
  (assoc job ::specs.job/assigned-agent agent-id))

(s/fdef job-with-agent-assigned
        :args (s/cat :agent-id ::specs.job-req/agent-id
                     :job ::specs.job/job))