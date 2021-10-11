(ns promecinia.core-test
  (:require [clojure.test :as t :refer [deftest testing is]]
            [com.walmartlabs.lacinia :refer [execute]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [promesa.core :as p]
            [promecinia.core :as core]))

(def star-wars-schema-edn
  '{:enums
    {:episode
     {:description "The episodes of the original Star Wars trilogy."
      :values [:NEWHOPE :EMPIRE :JEDI]}}

    :objects
    {:droid
     {:fields {:primary_functions {:type (list String)}
               :id {:type Int}
               :name {:type String}
               :appears_in {:type (list :episode)}
               :quotes {:type (list String)
                        :resolve :get-quotes}}}

     :human
     {:fields {:id {:type Int}
               :name {:type String}
               :home_planet {:type String}
               :appears_in {:type (list :episode)}
               :quotes {:type (list String)
                        :resolve :get-quotes}}}}

    :queries
    {:hero {:type (non-null :human)
            :args {:episode {:type :episode}}
            :resolve :get-hero}
     :droid {:type :droid
             :args {:id {:type Int :default-value 2001}}
             :resolve :get-droid}}})

(defn get-hero [_ arguments _]
  (let [{:keys [episode]} arguments]
    (-> (p/future
          (Thread/sleep 100)
          (if (= episode :NEWHOPE)
            {:id 1000
             :name "Luke"
             :home_planet "Tatooine"
             :appears_in ["NEWHOPE" "EMPIRE" "JEDI"]}
            {:id 2000
             :name "Lando Calrissian"
             :home_planet "Socorro"
             :appears_in ["EMPIRE" "JEDI"]}))
      (core/attach
        {:warning {:message "Try not. Do or do not. There is no try."}
         :context {:episode episode}}))))

(defn get-droid [_ arguments value]
  (p/future
    (Thread/sleep 100)
    (throw (ex-info
             "These aren’t the droids you’re looking for."
             {:arguments arguments :value value}))))

(defn get-quotes [{:keys [episode]} _ {:keys [id]}]
  (p/future
    (case [id episode]
      [1000 :NEWHOPE] ["But I was going into Tosche Station to pick up some power converters!"]
      [2000 :EMPIRE]  ["Now, take the wookiee and Leia to my ship."]
      [2000 :JEDI]    ["Home One, this is Gold Leader."])))

(def star-wars-schema
  (-> star-wars-schema-edn
    (attach-resolvers {:get-hero get-hero
                       :get-droid get-droid
                       :get-quotes get-quotes})
    schema/compile))

(def hero-query
  "query myQuery {
     hero (episode: NEWHOPE) {
       id
       name
       quotes
     }
   }")

(deftest get-hero-test
  (let [result (execute star-wars-schema hero-query nil nil)]
    (testing "on-deliver"
      (is (= "Luke" (get-in result [:data :hero :name]))))

    (testing "attach-context"
      (is (= ["But I was going into Tosche Station to pick up some power converters!"]
             (get-in result [:data :hero :quotes]))))

    (testing "attach-warning"
      (is (= "Try not. Do or do not. There is no try."
             (-> (get-in result [:extensions :warnings])
               first
               :message))))))

(def droid-query
  "query myQuery {
     droid (id: 1) {
       id
       name
     }
   }")

(deftest get-droid-test
  (testing "hide stack trace"
    (let [result (execute star-wars-schema droid-query nil nil)]
      (is (= "Internal Server Error."
             (-> (get-in result [:errors])
               first
               :message))))))
