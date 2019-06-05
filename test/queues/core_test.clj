(ns queues.core-test
  (:require [midje.sweet :refer :all]
            [queues.core :refer :all]))

(def new-agent-1
  {:new_agent {:id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
               :name "BoJack Horseman",
               :primary_skillset ["bills-questions"],
               :secondary_skillset []}})

(def new-job-1
  {:new_job {:id "f26e890b-df8e-422e-a39c-7762aa0bac36",
             :type "rewards-question",
             :urgent false}})

(def new-agent-2
  {:new_agent {:id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
               :name "Mr. Peanut Butter",
               :primary_skillset ["rewards-question"],
               :secondary_skillset ["bills-questions"]}})

(def new-job-2 {:new_job {:id "690de6bc-163c-4345-bf6f-25dd0c58e864",
                          :type "bills-questions",
                          :urgent false}})

(def new-job-3 {:new_job {:id "c0033410-981c-428a-954a-35dec05ef1d2",
                          :type "bills-questions",
                          :urgent true}})

(def job-request-1 {:job_request {:agent_id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}})

(def job-request-2 {:job_request {:agent_id  "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}})

(def job-assigned-1 {:job_assigned {:job_id "c0033410-981c-428a-954a-35dec05ef1d2",
                                  :agent_id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}})
(def job-assigned-2 {:job_assigned {:job_id "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                    :agent_id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}})

(facts "dequeue does not return a job-asigned until it has at least a new agent
       a new job and a job request that match each other"
       (fact "if receives an empty vector of events returns an empty vector"
             (dequeue []) => [])
       (fact "if only receives a new agent or a new job returns an empty vector
       even if they are compatible"
             (dequeue [new-agent-1]) => []
             (dequeue [new-agent-1 new-job-1]) => []
             (dequeue [new-agent-1 new-job-1 new-agent-2]) => []
             (dequeue [new-agent-1 new-job-1 new-agent-2 new-job-2]) => []
             (dequeue [new-agent-1 new-job-1 new-agent-2 new-job-2 new-job-3]) => [])
       (fact "if receives new agents, new jobs and job request that are compatible returns
       a job assigned"
             (dequeue [new-agent-1 new-job-1 new-agent-2 new-job-2 new-job-3
                       job-request-1]) => [job-assigned-1]
             (dequeue [new-agent-1 new-job-1 new-agent-2 new-job-2 new-job-3
                       job-request-1
                       job-request-2]) => [job-assigned-1 job-assigned-2])
       (fact "if receives a new agent, a job-request frmo that agent and two new jobs
       that are compatible only assigns the first job"
             (dequeue [new-agent-1 new-job-2 new-job-3
                       job-request-1]) => [job-assigned-1]))

(def agents-and-jobs-scheme
  {:agents []
   :jobs-assigned []
   :jobs-waiting []})

(facts "added-event "
       (fact ""
             (added-event agents-and-jobs-scheme new-agent-1) => (contains {:agents [(:new_agent new-agent-1)]})))