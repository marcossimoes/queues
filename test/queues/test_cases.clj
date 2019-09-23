(ns queues.test-cases
  ;; Although IDEA does not identify the sue of the spec requires bellow they ARE used, so DO NOT remove them
  (:require [queues.specs.agents :as specs.agents]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]))

;; To make this test more readable and concise it seemed right to create a abbreviation standard to make it
;; clear which skills and urgency each agent and job has at each time without being so verbose as to
;; clear which skills and urgency each agent and job has at each time without being so verbose as to
;; expose all the agent and job structure. The convention is the following
;;
;; agent w/ skills 1 and 2 in primary skillset and skills 3 and 4 in secondary skill-set: agent-p12-s34
;; agents w/ agent-p12-s34 and agent p34-s56: agents-p12s34-p34s56
;; job w/ type matching skill 1 and urgency true: job-1t
;; jobs containing jobs 1t, 2t and 3f: jobs-1t2t3f

;; ########## Agent #############

(def skill-1 "bills-questions")
(def skill-2 "rewards-question")
(def skill-5=skill-1 skill-1)
(def skill-6 "acquisition-questions")

(def agent-p1-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260")
(def agent-p1-name "BoJack Horseman")
(def agent-p1-str (str "{\n    \"new_agent\": {\n      \"id\": \""
                       agent-p1-id
                       "\",\n      \"name\": \""
                       agent-p1-name
                       "\",\n      \"primary_skillset\": [\""
                       skill-1
                       "\"],\n      \"secondary_skillset\": []\n    }\n  }\n  "))
(def new-agent-json-event-payload-p1 {"id"                 agent-p1-id
                                      "name"               agent-p1-name
                                      "primary_skillset"   [skill-1]
                                      "secondary_skillset" []})
(def new-agent-json-event-type-p1 "new_agent")
(def new-agent-json-event-p1 {new-agent-json-event-type-p1 new-agent-json-event-payload-p1})
(def agent-p1 #::specs.agents{:id                 agent-p1-id,
                              :name               agent-p1-name,
                              :primary-skillset   [skill-1],
                              :secondary-skillset []})
(def new-agent-clj-event-p1 #::specs.events{:new-agent agent-p1})

(def job-req-json-event-str-p1 (str "{\n    \"job_request\": {\n      \"agent_id\": \""
                                    agent-p1-id
                                    "\"\n    }\n  }"))
(def job-req-json-event-payload-p1 {"agent_id" agent-p1-id})
(def job-req-json-event-type-p1 "job_request")
(def job-req-json-event-p1 {job-req-json-event-type-p1 job-req-json-event-payload-p1})
(def job-req-p1 #::specs.job-request{:agent-id agent-p1-id})
(def job-req-clj-event-p1 #::specs.events{:job-request job-req-p1})

(def agent-p2-s1-id "ed0e23ef-6c2b-430c-9b90-cd4f1ff74c88")
(def agent-p2-s1-name "Mr. Peanut Butter")
(def new-agent-json-event-str-p2-s1 (str "{\n    \"new_agent\": {\n      \"id\": \""
                                         agent-p2-s1-id
                                         "\",\n      \"name\": \""
                                         agent-p2-s1-name
                                         "\",\n      \"primary_skillset\": [\""
                                         skill-2
                                         "\"],\n      \"secondary_skillset\": [\""
                                         skill-1
                                         "\"]\n    }\n  }"))
(def agent-p2-s1 #::specs.agents{:id                 agent-p2-s1-id,
                                 :name               agent-p2-s1-name,
                                 :primary-skillset   [skill-2],
                                 :secondary-skillset [skill-1]})
(def new-agent-clj-event-p2-s1 #::specs.events{:new-agent agent-p2-s1})
(def job-req-json-str-p2-s1 (str "{\n    \"job_request\": {\n      \"agent_id\": \""
                                 agent-p2-s1-id
                                 "\"\n    }\n  }"))
(def job-req-clj-event-p2-s1 #::specs.job-request{:agent-id agent-p2-s1-id})

(def agent-p1-s6-id "4")
(def agent-p1-s6 #::specs.agents{:id                 agent-p1-s6-id,
                                 :name               "Gabriela Lima",
                                 :primary-skillset   [skill-1],
                                 :secondary-skillset [skill-6]})
(def job-req-p1-s6 #::specs.job-request{:agent-id agent-p1-s6-id})

(def agent-p5=1-id "7")
(def agent-p5=1 #::specs.agents{:id                 agent-p5=1-id,
                                :name               "Gabriela Lima",
                                :primary-skillset   [skill-5=skill-1],
                                :secondary-skillset []})
(def job-req-p5=1 #::specs.job-request{:agent-id agent-p5=1-id})

(def agent-p12-id "3")

(def agent-p12 #::specs.agents{:id                 agent-p12-id,
                               :name               "Gabriela Lima",
                               :primary-skillset   [skill-1 skill-2],
                               :secondary-skillset []})
(def job-req-p12 #::specs.job-request{:agent-id agent-p12-id})

;; ########## Job #############

(def job-type-1 skill-1)
(def job-type-2 skill-2)
(def job-type-5=job-type-1 skill-5=skill-1)

(def job-id-1f "690de6bc-163c-4345-bf6f-25dd0c58e864")
(def new-job-json-str-1f (str "{\n    \"new_job\": {\n      \"id\": \""
                              job-id-1f
                              "\",\n      \"type\": \""
                              job-type-1
                              "\",\n      \"urgent\": false\n    }\n  }"))
(def job-1f #::specs.job{:id     job-id-1f,
                         :type   job-type-1,
                         :urgent false})
(def new-job-clj-event-1f #::specs.events{:new-job job-1f})
(def job-1f-waiting job-1f)

(def job-id-1t "c0033410-981c-428a-954a-35dec05ef1d2")
(def new-job-json-str-1t (str "{\n    \"new_job\": {\n      \"id\": \""
                              job-id-1t
                              "\",\n      \"type\": \""
                              job-type-1
                              "\",\n      \"urgent\": true\n    }\n  }"))
(def new-job-json-payload-1t {"id"     job-id-1t
                              "type"   job-type-1
                              "urgent" true})
(def new-job-json-type-1t "new_job")
(def new-job-json-event-1t {new-job-json-type-1t new-job-json-payload-1t})
(def job-1t #::specs.job{:id job-id-1t :type job-type-1 :urgent true})
(def new-job-clj-event-1t #::specs.events{:new-job job-1t})
(def job-1t-started (assoc job-1t ::specs.job/assigned-agent agent-p1-id))

(def job-id-2f "f26e890b-df8e-422e-a39c-7762aa0bac36")
(def new-job-json-str-2f (str "{\n    \"new_job\": {\n      \"id\": \""
                              job-id-2f
                              "\",\n      \"type\": \""
                              job-type-2
                              "\",\n      \"urgent\": false\n    }\n  }"))
(def job-2f #::specs.job{:id     job-id-2f,
                         :type   job-type-2,
                         :urgent false})
(def new-job-clj-event-2f #::specs.events{:new-job job-2f})
(def job-2f-started (assoc job-2f ::specs.job/assigned-agent agent-p2-s1-id))

(def job-2t #::specs.job{:id "2" :type job-type-2 :urgent true})
(def new-job-clj-event-2t #::specs.events{:new-job job-2t})

(def job-5t=1t #::specs.job{:id "5" :type job-type-5=job-type-1 :urgent true})

;; ########## Events #############

(def json-events-str (str "[\n"
                          agent-p1-str ",\n"
                          new-job-json-str-2f ",\n"
                          new-agent-json-event-str-p2-s1 ",\n"
                          new-job-json-str-1f ",\n"
                          new-job-json-str-1t ",\n"
                          job-req-json-event-str-p1 ",\n"
                          job-req-json-str-p2-s1 "\n"
                          "]"))


(def json-events [{"new_agent" {"id"                 agent-p1-id,
                                "name"               agent-p1-name,
                                "primary_skillset"   [skill-1],
                                "secondary_skillset" []}},
                  {"new_job" {"id"     job-id-2f,
                              "type"   job-type-2,
                              "urgent" false}},
                  {"new_agent" {"id"                 agent-p2-s1-id,
                                "name"               agent-p2-s1-name,
                                "primary_skillset"   [job-type-2],
                                "secondary_skillset" [job-type-1]}},
                  {"new_job" {"id"     job-id-1f,
                              "type"   job-type-1,
                              "urgent" false}},
                  {"new_job" {"id"     job-id-1t,
                              "type"   job-type-1,
                              "urgent" true}},
                  {"job_request" {"agent_id" agent-p1-id}},
                  {"job_request" {"agent_id" agent-p2-s1-id}}])

;; ########## Queues and DB #############

(def agents-empty {})
(def agents-p1-p12 [agent-p1 agent-p12])
(def agents-p1-p12-p1s6 [agent-p1 agent-p12 agent-p1-s6])

(def job-queue [job-1t])

(def job-req-p2-s1 #::specs.job-request{:agent-id agent-p2-s1-id})
(def job-req-clj-event-p2-s1 #::specs.events{:job-request job-req-p2-s1})


(def clj-events [new-agent-clj-event-p1,
                 new-job-clj-event-2f,
                 new-agent-clj-event-p2-s1,
                 new-job-clj-event-1f,
                 new-job-clj-event-1t,
                 job-req-clj-event-p1
                 job-req-clj-event-p2-s1])

(def jobs-waiting-empty [])
(def jobs-waiting [job-1f-waiting])

(def jobs-in-progress-empty [])
(def jobs-in-progress [job-1t-started job-2f-started])

(def jobs-done-empty [])

(def job-reqs-queued-empty [])
(def job-reqs-p1-p12-p1s6 [job-req-p1 job-req-p12 job-req-p1-s6])

(def job-1t-assigned-to-agent-p1-clj-event #::specs.job-assigned{:job-assigned #::specs.job-assigned{:job-id job-id-1t, :agent-id agent-p1-id}})
(def job-2f-assigned-to-agent-p2-s1-clj-event #::specs.job-assigned{:job-assigned #::specs.job-assigned{:job-id job-id-2f, :agent-id agent-p2-s1-id}})

(def jobs-assigned-clj-events [job-1t-assigned-to-agent-p1-clj-event job-2f-assigned-to-agent-p2-s1-clj-event])
(def jobs-assigned-clj-events-output-to-cli (str "[#:queues.specs.job-assigned{:job-assigned\n                             "
                                                 "#:queues.specs.job-assigned{:job-id\n                                                         \""
                                                 job-id-1t
                                                 "\",\n                                                         "
                                                 ":agent-id\n                                                         \""
                                                 agent-p1-id "\"}}\n "
                                                 "#:queues.specs.job-assigned{:job-assigned\n                             #:queues.specs.job-assigned{:job-id\n                                                         \""
                                                 job-id-2f
                                                 "\",\n                                                         :agent-id\n                                                         \""
                                                 agent-p2-s1-id
                                                 "\"}}]\n"))

(def jobs-assigned-json-events-str (str "[ {\n  \"job_assigned\" : {\n    \"job_id\" : \""
                                        job-id-1t
                                        "\",\n    \"agent_id\" : \""
                                        agent-p1-id
                                        "\"\n  }\n}, {\n  \"job_assigned\" : {\n    \"job_id\" : \""
                                        job-id-2f
                                        "\",\n    \"agent_id\" : \""
                                        agent-p2-s1-id
                                        "\"\n  }\n} ]"))


;; ########## Broke on purpose #############

(def skill-1 "bills-questions")
(def reviewer-p1-id "1")
(def reviewer-p1-name "Sergio Moro")
(def json-event-str-wo-respective-clj-event (str "{\n    \"new_reviewer\": {\n      \"id\": \""
                                                 reviewer-p1-id
                                                 "\",\n      \"name\": \""
                                                 reviewer-p1-name
                                                 "\",\n      \"primary_skillset\": [\""
                                                 skill-1
                                                 "\"],\n      \"secondary_skillset\": []\n    }\n  }\n  "))
(def mal-formatted-json-event (->> agent-p1-str
                                   (drop-last 10)
                                   (apply str)))