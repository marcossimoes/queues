(ns queues.io-test
  (:require [cheshire.core :refer :all]
            [clojure.spec.test.alpha :as stest]
            [midje.sweet :refer :all]
            [queues.io :refer :all]
            [queues.init :as init]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]))

(let [json-str-new-agent-1 "{
                                      \"new_agent\": {
                                                      \"id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\",
                                                      \"name\": \"BoJack Horseman\",
                                                      \"primary_skillset\": [\"bills-questions\"],
                                                      \"secondary_skillset\": []
                                                      }
                                     }"
      json-str-new-job-1 "{
                                    \"new_job\": {
                                                  \"id\": \"f26e890b-df8e-422e-a39c-7762aa0bac36\",
                                                  \"type\": \"rewards-question\",
                                                  \"urgent\": false
                                                 }
                                   }"
      json-str-new-agent-2 "{
                                      \"new_agent\": {
                                                      \"id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\",
                                                      \"name\": \"Mr. Peanut Butter\",
                                                      \"primary_skillset\": [\"rewards-question\"],
                                                      \"secondary_skillset\": [\"bills-questions\"]
                                                     }
                                     }"
      json-str-new-job-2 "{
                                    \"new_job\": {
                                                  \"id\": \"690de6bc-163c-4345-bf6f-25dd0c58e864\",
                                                  \"type\": \"bills-questions\",
                                                  \"urgent\": false
                                                 }
                                   }"
      json-str-new-job-3 "{
                                    \"new_job\": {
                                                  \"id\": \"c0033410-981c-428a-954a-35dec05ef1d2\",
                                                  \"type\": \"bills-questions\",
                                                  \"urgent\": true
                                                 }
                                   }"
      json-str-job-request-1 "{
                                       \"job_request\": {
                                                         \"agent_id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\"
                                                       }
                                       }"
      json-str-job-request-2 "{
                                        \"job_request\": {
                                                          \"agent_id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\"
                                                         }
                                       }"
      json-events (str "["
                       json-str-new-agent-1 ","
                       json-str-new-job-1 ","
                       json-str-new-agent-2 ","
                       json-str-new-job-2 ","
                       json-str-new-job-3 ","
                       json-str-job-request-1 ","
                       json-str-job-request-2
                       "]")
      json-new-agent-1 {"new_agent" {"id"                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                     "name"               "BoJack Horseman",
                                     "primary_skillset"   ["bills-questions"],
                                     "secondary_skillset" []}}
      json-new-job-1 {"new_job" {"id"     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                 "type"   "rewards-question",
                                 "urgent" false}}
      json-new-agent-2 {"new_agent" {"id"                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88",
                                     "name"               "Mr. Peanut Butter",
                                     "primary_skillset"   ["rewards-question"],
                                     "secondary_skillset" ["bills-questions"]}}
      json-new-job-2 {"new_job" {"id"     "690de6bc-163c-4345-bf6f-25dd0c58e864",
                                 "type"   "bills-questions",
                                 "urgent" false}}

      json-new-job-3 {"new_job" {"id"     "c0033410-981c-428a-954a-35dec05ef1d2",
                                 "type"   "bills-questions",
                                 "urgent" true}}
      json-job-request-1 {"job_request" {"agent_id" "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
      json-job-request-2 {"job_request" {"agent_id" "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}
      clj-new-agent-1 {::specs.events/new-agent {::specs.agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                                           ::specs.agent/name               "BoJack Horseman"
                                           ::specs.agent/primary-skillset   ["bills-questions"]
                                           ::specs.agent/secondary-skillset []}}
      clj-new-job-1 {::specs.events/new-job {::specs.job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36"
                                       ::specs.job/type   "rewards-question"
                                       ::specs.job/urgent false}}
      clj-new-agent-2 {::specs.events/new-agent {::specs.agent/id                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"
                                           ::specs.agent/name               "Mr. Peanut Butter"
                                           ::specs.agent/primary-skillset   ["rewards-question"]
                                           ::specs.agent/secondary-skillset ["bills-questions"]}}
      clj-new-job-2 {::specs.events/new-job {::specs.job/id     "690de6bc-163c-4345-bf6f-25dd0c58e864"
                                       ::specs.job/type   "bills-questions"
                                       ::specs.job/urgent false}}
      clj-new-job-3 {::specs.events/new-job {::specs.job/id     "c0033410-981c-428a-954a-35dec05ef1d2"
                                       ::specs.job/type   "bills-questions"
                                       ::specs.job/urgent true}}
      clj-job-request-1 {::specs.events/job-request{::specs.job-request/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
      clj-job-request-2 {::specs.events/job-request{::specs.job-request/agent-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}
      clj-events [clj-new-agent-1
                  clj-new-job-1
                  clj-new-agent-2
                  clj-new-job-2
                  clj-new-job-3
                  clj-job-request-1
                  clj-job-request-2]
      clj-new-agent-content-1 {::specs.agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                               ::specs.agent/name               "BoJack Horseman"
                               ::specs.agent/primary-skillset   ["bills-questions"]
                               ::specs.agent/secondary-skillset []}
      clj-new-agent-content-2 {::specs.agent/id                 "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"
                               ::specs.agent/name               "Mr. Peanut Butter"
                               ::specs.agent/primary-skillset   ["rewards-question"]
                               ::specs.agent/secondary-skillset ["bills-questions"]}
      clj-new-job-content-1 {::specs.job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36"
                             ::specs.job/type   "rewards-question"
                             ::specs.job/urgent false}
      clj-new-job-content-2 {::specs.job/id     "690de6bc-163c-4345-bf6f-25dd0c58e864"
                             ::specs.job/type   "bills-questions"
                             ::specs.job/urgent false}
      clj-new-job-content-3 {::specs.job/id     "c0033410-981c-428a-954a-35dec05ef1d2"
                             ::specs.job/type   "bills-questions"
                             ::specs.job/urgent true}
      clj-job-request-content-1 {::specs.job-request/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}
      clj-job-request-content-2 {::specs.job-request/agent-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}]

  (facts "js-kw->cj-kw"
         (fact "receives keys as strings and returns them as keywords"
               (js-kw->cj-kw "my" "name") => :queues.specs.my/name)
         (fact "if keys are composed by _ replaces them for -"
               (js-kw->cj-kw "my" "first_name") => :queues.specs.my/first-name))

  (facts "kworded-content"
         (fact "Receives a 'agent' flag and a 'queues.io-formatted-agent'
         and returns a 'clj-formatted-agent` with namespaced kwds"
               (typed-kworded-content "agent" json-new-agent-1) => clj-new-agent-content-1
               (typed-kworded-content "agent" json-new-agent-2) => clj-new-agent-content-2)
         (fact "Receives a 'job' flag and a 'queues.io-formatted-agent'
         and returns a 'clj-formatted-agent` with namespaced kwds"
               (typed-kworded-content "job" json-new-job-1) => clj-new-job-content-1
               (typed-kworded-content "job" json-new-job-2) => clj-new-job-content-2
               (typed-kworded-content "job" json-new-job-3) => clj-new-job-content-3)
         (fact "Receives a 'job-request' flag and a 'queues.io-formatted-agent'
         and returns a 'clj-formatted-agent` with namespaced kwds"
               (typed-kworded-content "job-request" json-job-request-1) => clj-job-request-content-1
               (typed-kworded-content "job-request" json-job-request-2) => clj-job-request-content-2))

  (facts "read-queues.io-event"
         (fact "Receives an empty vector and a new agent queues.io formatted event
       and returns a vector with the new agent clj formatted"
               (read-json-event [] json-new-agent-1) => [clj-new-agent-1])
         (fact "Receives an empty vector and a new job queues.io formatted
       and returns a vector with the new job clj formatted"
               (read-json-event [] json-new-job-1) => [clj-new-job-1])
         (fact "Receives an empty vector and a job request queues.io formatted
       and returns a vector with the job request clj formatted"
               (read-json-event [] json-job-request-1) => [clj-job-request-1])
         (fact "Receives a vector and a new agent queues.io formatted event
       and returns a vector with the new agent clj formatted added at the end"
               (read-json-event [1 2] json-new-agent-1) => [1 2 clj-new-agent-1])
         (fact "Receives a vector and a new job queues.io formatted
       and returns a vector with the new job clj formatted added at the end"
               (read-json-event [1 2] json-new-job-1) => [1 2 clj-new-job-1])
         (fact "Receives a vector and a job request queues.io formatted
       and returns a vector with the job request clj formatted added at the end"
               (read-json-event [1 2] json-job-request-1) => [1 2 clj-job-request-1]))

  (facts "read-queues.io-events"
         (fact "Receives a vector containing a new agent queues.io formatted
       and returns a vector with a new agent clj formatted"
               (read-json-events (str "[" json-str-new-agent-1 "]")) => [clj-new-agent-1])
         (fact "Receives a vector containing a new job queues.io formatted
       and returns a vector with a new job clj formatted"
               (read-json-events (str "[" json-str-new-job-1 "]")) => [clj-new-job-1])
         (fact "Receives a vector containing a job request queues.io formatted
       and returns a vector with a job request clj formatted"
               (read-json-events (str "[" json-str-job-request-1 "]")) => [clj-job-request-1])
         (fact "Receives a vector containing a multiple different events queues.io formatted
       and returns a vector with a multiple different events clj formatted"
               (read-json-events (str "[" json-str-new-agent-1 "," json-str-new-job-1 "]")) => [clj-new-agent-1 clj-new-job-1]
               (read-json-events json-events) => clj-events)))

(facts "write-queues.io-events"
       (fact "Receives a string with clojure formatted events
       and returns a vector with queues.io formatted events"
             (write-json-events [{::specs.job-assigned/job-assigned {::specs.job-assigned/job-id "123"
                                                                     ::specs.job-assigned/agent-id "456"}}])
             => "[ {\n  \"job_assigned\" : {\n    \"job_id\" : \"123\",\n    \"agent_id\" : \"456\"\n  }\n} ]"))

;; TODO: create test for typed-kworded-content
;; TODO: create test for read-json-file
;; TODO: create test for write-json-file

(stest/instrument)