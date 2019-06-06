(ns queues.core-test
  (:require [midje.sweet :refer :all]
            [queues.core :refer :all]
            [queues.models.events :as events]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.agents-and-jobs :as aajs]
            [queues.models.job-assigned :as ja]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(def new-agent-1 {::events/new-agent {::agent/id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                      ::agent/name "BoJack Horseman",
                                      ::agent/primary-skillset ["bills-questions"],
                                      ::agent/secondary-skillset []}})

(def new-job-1 {::events/new-job {::job/id "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                  ::job/type "rewards-question",
                                  ::job/urgent false}})

(def new-agent-2 {::events/new-agent {::agent/id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
                                      ::agent/name "Mr. Peanut Butter",
                                      ::agent/primary-skillset ["rewards-question"],
                                      ::agent/secondary-skillset ["bills-questions"]}})

(def new-job-2 {::events/new-job {::job/id "690de6bc-163c-4345-bf6f-25dd0c58e864",
                                  ::job/type "bills-questions",
                                  ::job/urgent false}})

(def new-job-3 {::events/new-job {::job/id "c0033410-981c-428a-954a-35dec05ef1d2",
                                  ::job/type "bills-questions",
                                  ::job/urgent true}})

(def job-request-1 {::events/job-request {:agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}})

(def job-request-2 {::events/job-request {:agent-id  "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}})

(def job-assigned-1 {::events/job-assigned {:job-id "c0033410-981c-428a-954a-35dec05ef1d2",
                                            :agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}})
(def job-assigned-2 {::events/job-assigned {:job-id "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                            :agent-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}})

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
  {::aajs/agents []
   ::aajs/jobs-assigned []
   ::aajs/jobs-waiting []})

(facts "added-event"
       (fact "Adds new agents and new jobs to their respective queues in agents and jobs"
             (added-event agents-and-jobs-scheme new-agent-1) => (contains {::aajs/agents [(::events/new-agent new-agent-1)]})
             (added-event agents-and-jobs-scheme new-job-1) => (contains {::aajs/jobs-waiting [(::events/new-job new-job-1)]})))

(def job-sample
  (gen/generate (s/gen ::job/job)))

(def agent-sample
  (-> (gen/generate (s/gen ::agent/agent))
      (assoc ::agent/primary-skillset [(::job/type job-sample)])))

(def job-req-content-sample
  {::agent/id (::agent/id agent-sample)})

(def aajs-sample
  (-> agents-and-jobs-scheme
      (update ::aajs/agents conj agent-sample)
      (update ::aajs/jobs-waiting conj job-sample)))

(def job-assigned-sample
  {::ja/job-assigned {::job/id   (::job/id job-sample)
                        ::agent/id (::agent/id agent-sample)}})

(def job-assigned-aajs-sample
  (update aajs-sample ::aajs/jobs-assigned conj job-assigned-sample))

(facts "agent-found"
       (fact "if agents and jobs has the provided agent id returns agent"
         (agent-found aajs-sample job-req-content-sample) => agent))

(def aajs-sample-2
  (-> agents-and-jobs-scheme
      (update ::aajs/agents conj agent-sample)))

(facts "matching waiting job"
       (fact "if there are no jobs in jobs-waiting that suit agent returns nil"
             (matching-waiting-job aajs-sample-2 job-req-content-sample) => nil))

(facts "update-job-assigneds-func"
       (fact
         (update
           aajs-sample
           ::aajs/jobs-assigned
           (update-job-assigneds-func job-sample agent-sample))
         =>
         job-assigned-aajs-sample))

(facts "job-not-matches?"
       (fact "if both type and urgent matches return false"
         (job-not-matches? "rewards" true {::job/type "rewards"
                                           ::job/urgent true})
         => false)
       (fact "if type or urgent does not match returns true"
             (job-not-matches? "rewards" true {::job/type "bills"
                                               ::job/urgent true})
             => true
             (job-not-matches? "rewards" true {::job/type "rewards"
                                               ::job/urgent false})
             => true)
       (fact "if both do not match match returns true"
             (job-not-matches? "rewards" true {::job/type "bills"
                                               ::job/urgent false})
             => true))

(facts "job-with-prior"
       (fact "if no job matches either skill, urgency or both returns nil"
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     true})
             => nil
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => nil
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     true})
             => nil
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => nil
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     true})
             => nil
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 2 ::job/type "rewards" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => nil)
       (fact "if job matches both skill, urgency or both returns the job"
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     true})
             => {::job/id 1 ::job/type "rewards" ::job/urgent true}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => {::job/id 3 ::job/type "bills" ::job/urgent true}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     false})
             => {::job/id 2 ::job/type "rewards" ::job/urgent false}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     false})
             => {::job/id 4 ::job/type "bills" ::job/urgent false})
       (fact "Handles agents that have no secondary skillset"
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset []}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => nil)
       (fact "Returns the first and only the first matching job"
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}
                              {::job/id 5 ::job/type "rewards" ::job/urgent true}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     true})
             => {::job/id 1 ::job/type "rewards" ::job/urgent true}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}
                              {::job/id 7 ::job/type "bills" ::job/urgent true}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     true})
             => {::job/id 3 ::job/type "bills" ::job/urgent true}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}
                              {::job/id 6 ::job/type "rewards" ::job/urgent false}]
                             {:skill-type ::agent/primary-skillset
                              :urgent     false})
             => {::job/id 2 ::job/type "rewards" ::job/urgent false}
             (job-with-prior {::agent/primary-skillset   ["rewards"]
                              ::agent/secondary-skillset ["bills"]}
                             [{::job/id 1 ::job/type "rewards" ::job/urgent true}
                              {::job/id 2 ::job/type "rewards" ::job/urgent false}
                              {::job/id 3 ::job/type "bills" ::job/urgent true}
                              {::job/id 4 ::job/type "bills" ::job/urgent false}
                              {::job/id 8 ::job/type "bills" ::job/urgent false}]
                             {:skill-type ::agent/secondary-skillset
                              :urgent     false})
             => {::job/id 4 ::job/type "bills" ::job/urgent false}))

(facts "job-found"
       (fact "Returns nil if there are not jobs that match the agent skills"
         (job-found [{::job/id 9 ::job/type "cb" ::job/urgent true}
                     {::job/id 10 ::job/type "cb" ::job/urgent false}
                     {::job/id 11 ::job/type "acq" ::job/urgent true}
                     {::job/id 12 ::job/type "acq" ::job/urgent false}
                     {::job/id 13 ::job/type "cb" ::job/urgent true}
                     {::job/id 14 ::job/type "cb" ::job/urgent false}
                     {::job/id 15 ::job/type "acq" ::job/urgent true}
                     {::job/id 16 ::job/type "acq" ::job/urgent false}]
                    {::agent/primary-skillset   ["rewards"]
                     ::agent/secondary-skillset ["bills"]})
             => nil)
       (fact "Returns the first jobs with right priority.
       primary-urgent > primary > secondary-urgent > secondary"
             (job-found [{::job/id 3 ::job/type "bills" ::job/urgent true}
                         {::job/id 7 ::job/type "bills" ::job/urgent true}
                         {::job/id 1 ::job/type "rewards" ::job/urgent true}
                         {::job/id 5 ::job/type "rewards" ::job/urgent true}
                         {::job/id 2 ::job/type "rewards" ::job/urgent false}
                         {::job/id 4 ::job/type "bills" ::job/urgent false}
                         {::job/id 6 ::job/type "rewards" ::job/urgent false}
                         {::job/id 8 ::job/type "bills" ::job/urgent false}]
                        {::agent/primary-skillset   ["rewards"]
                         ::agent/secondary-skillset ["bills"]})
             => {::job/id 1 ::job/type "rewards" ::job/urgent true}
             (job-found [{::job/id 3 ::job/type "bills" ::job/urgent true}
                         {::job/id 7 ::job/type "bills" ::job/urgent true}
                         {::job/id 2 ::job/type "rewards" ::job/urgent false}
                         {::job/id 4 ::job/type "bills" ::job/urgent false}
                         {::job/id 6 ::job/type "rewards" ::job/urgent false}
                         {::job/id 8 ::job/type "bills" ::job/urgent false}]
                        {::agent/primary-skillset   ["rewards"]
                         ::agent/secondary-skillset ["bills"]})
             => {::job/id 2 ::job/type "rewards" ::job/urgent false}
             (job-found [{::job/id 3 ::job/type "bills" ::job/urgent true}
                         {::job/id 7 ::job/type "bills" ::job/urgent true}
                         {::job/id 4 ::job/type "bills" ::job/urgent false}
                         {::job/id 8 ::job/type "bills" ::job/urgent false}]
                        {::agent/primary-skillset   ["rewards"]
                         ::agent/secondary-skillset ["bills"]})
             => {::job/id 3 ::job/type "bills" ::job/urgent true}
             (job-found [{::job/id 4 ::job/type "bills" ::job/urgent false}
                         {::job/id 8 ::job/type "bills" ::job/urgent false}]
                        {::agent/primary-skillset   ["rewards"]
                         ::agent/secondary-skillset ["bills"]})
             => {::job/id 4 ::job/type "bills" ::job/urgent false})
       (fact "Returns nil if there are no jobs in the waiting list"
             (job-found []
                        {::agent/primary-skillset   ["rewards"]
                         ::agent/secondary-skillset ["bills"]})
             => nil))

;;TODO: create tests for job-found