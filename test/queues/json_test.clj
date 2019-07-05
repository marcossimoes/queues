(ns queues.json-test
  (:require [midje.sweet :refer :all]
            [queues.json :refer :all]
            [cheshire.core :refer :all]
            [queues.models.job :as job]
            [queues.models.job-assigned :as ja]
            [queues.models.job-request :as jr]
            [queues.models.agent :as agent]
            [queues.models.events :as events]))
(facts "js-kw->cj-kw"
       (fact "receives keys as strings and returns them as keywords"
             (js-kw->cj-kw "my" "name") => :queues.models.my/name)
       (fact "if keys are composed by _ replaces them for -"
             (js-kw->cj-kw "my" "first_name") => :queues.models.my/first-name))
;;(facts "event-content"
;;       (fact ""))
;;(facts "kworded-content"
;;       )
;;(facts "typed-kworded-content"
;;       )
;;(facts "read-json-event"
;;       )

(let [json-new-agent-1 "{
                                      \"new_agent\": {
                                                      \"id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\",
                                                      \"name\": \"BoJack Horseman\",
                                                      \"primary_skillset\": [\"bills-questions\"],
                                                      \"secondary_skillset\": []
                                                      }
                                     }"
      json-new-job-1 "{
                                    \"new_job\": {
                                                  \"id\": \"f26e890b-df8e-422e-a39c-7762aa0bac36\",
                                                  \"type\": \"rewards-question\",
                                                  \"urgent\": false
                                                 }
                                   }"
      json-new-agent-2 "{
                                      \"new_agent\": {
                                                      \"id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\",
                                                      \"name\": \"Mr. Peanut Butter\",
                                                      \"primary_skillset\": [\"rewards-question\"],
                                                      \"secondary_skillset\": [\"bills-questions\"]
                                                     }
                                     }"
      json-new-job-2 "{
                                    \"new_job\": {
                                                  \"id\": \"690de6bc-163c-4345-bf6f-25dd0c58e864\",
                                                  \"type\": \"bills-questions\",
                                                  \"urgent\": false
                                                 }
                                   }"
      json-new-job-3 "{
                                    \"new_job\": {
                                                  \"id\": \"c0033410-981c-428a-954a-35dec05ef1d2\",
                                                  \"type\": \"bills-questions\",
                                                  \"urgent\": true
                                                 }
                                   }"
      json-job-request-1 "{
                                       \"job_request\": {
                                                         \"agent_id\": \"8ab86c18-3fae-4804-bfd9-c3d6e8f66260\"
                                                       }
                                       }"
      json-job-request-2 "{
                                        \"job_request\": {
                                                          \"agent_id\": \"ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88\"
                                                         }
                                       }"
      json-events (str "["
                       json-new-agent-1 ","
                       json-new-job-1 ","
                       json-new-agent-2 ","
                       json-new-job-2 ","
                       json-new-job-3 ","
                       json-job-request-1 ","
                       json-job-request-2
                       "]")
      clj-new-agent-1 {::events/new-agent {::agent/id               "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"
                                           ::agent/name             "BoJack Horseman"
                                           ::agent/primary-skillset ["bills-questions"]
                                           ::agent/secondary-skillset []}}
      clj-new-job-1 {::events/new-job {::job/id "f26e890b-df8e-422e-a39c-7762aa0bac36"
                                       ::job/type "rewards-question"
                                       ::job/urgent false}}
      clj-new-agent-2  {::events/new-agent {::agent/id               "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"
                                            ::agent/name             "Mr. Peanut Butter"
                                            ::agent/primary-skillset ["rewards-question"]
                                            ::agent/secondary-skillset ["bills-questions"]}}
      clj-new-job-2  {::events/new-job {::job/id   "690de6bc-163c-4345-bf6f-25dd0c58e864"
                                        ::job/type "bills-questions"
                                        ::job/urgent false}}
      clj-new-job-3 {::events/new-job {::job/id   "c0033410-981c-428a-954a-35dec05ef1d2"
                                       ::job/type "bills-questions"
                                       ::job/urgent true}}
      clj-job-request-1 {::events/job-request {::jr/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
      clj-job-request-2  {::events/job-request {::jr/agent-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88"}}
      clj-events [clj-new-agent-1
                  clj-new-job-1
                  clj-new-agent-2
                  clj-new-job-2
                  clj-new-job-3
                  clj-job-request-1
                  clj-job-request-2]]
  (facts "read-json-events"
         (fact "Receives a vector containing a new agent json formatted
       and returns a vector with a new agent clj formatted"
               (read-json-events (str "[" json-new-agent-1 "]")) => [clj-new-agent-1])
         (fact "Receives a vector containing a new job json formatted
       and returns a vector with a new job clj formatted"
               (read-json-events (str "[" json-new-job-1 "]")) => [clj-new-job-1])
         (fact "Receives a vector containing a job request json formatted
       and returns a vector with a job request clj formatted"
               (read-json-events (str "[" json-job-request-1 "]")) => [clj-job-request-1])
         (fact "Receives a vector containing a multiple different events json formatted
       and returns a vector with a multiple different events clj formatted"
               (read-json-events (str "[" json-new-agent-1 "," json-new-job-1 "]")) => [clj-new-agent-1 clj-new-job-1]
               (read-json-events json-events) => clj-events))
  (facts "read-json-event"
         (fact "Receives an empty vector and a new agent json formatted event
       and returns a vector with the new agent clj formatted"
               (read-json-event [] (parse-string json-new-agent-1)) => [clj-new-agent-1])
         (fact "Receives an empty vector and a new job json formatted
       and returns a vector with the new job clj formatted"
               (read-json-event [] (parse-string json-new-job-1)) => [clj-new-job-1])
         (fact "Receives an empty vector and a job request json formatted
       and returns a vector with the job request clj formatted"
               (read-json-event [] (parse-string json-job-request-1)) => [clj-job-request-1])
         (fact "Receives a vector and a new agent json formatted event
       and returns a vector with the new agent clj formatted added at the end"
               (read-json-event [] (parse-string json-new-agent-1)) => [clj-new-agent-1])
         (fact "Receives a vector and a new job json formatted
       and returns a vector with the new job clj formatted added at the end"
               (read-json-event [] (parse-string json-new-job-1)) => [clj-new-job-1])
         (fact "Receives a vector and a job request json formatted
       and returns a vector with the job request clj formatted added at the end"
               (read-json-event [] (parse-string json-job-request-1)) => [clj-job-request-1])))
(facts "write-json-events"
       (fact "Receives a string with clojure formatted events
       and returns a vector with json formatted events"
             (write-json-events [{::ja/job-assigned {::job/id "123"
                                                     ::jr/agent-id "456"}}])
             => "[ {\n  \"job_assigned\" : {\n    \"job_id\" : \"123\",\n    \"agent_id\" : \"456\"\n  }\n} ]"))