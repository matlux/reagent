(ns simpleexample.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! take!]]
            [ajax.core :refer [GET POST]]
            [reagent.core :as r]
            [simpleexample.components :as c]
            [simpleexample.geometry :as g])
  )

(comment

  (use 'figwheel-sidecar.repl-api)
  (start-figwheel!)
  (cljs-repl)

  (in-ns 'simpleexample.core)
  *ns*
  (println "hiya2")
  (http/get "https://api.github.com/users"
            {:with-credentials? false
             :query-params      {"since" 135}})

  (def c (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response))))))
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %))

  (take! c #(println %))
  (GET "http://localhost:8080/bar")
  (foo)
  @points

  (map (fn [p1 p2] [p1 p2]) '(1 2 3 4) (drop 1 '(1 2 3 4)))

  (conj [1 2 3] 4)
  (type (js->clj (get-point)))
  (type 4)

  (->>
    (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] '([1 2] [2 3] [3 4]))
    second;
    rescale
    )

  (->> (map (fn [p1 p2] [p1 p2]) '(1 2 3 4 5) (drop 1 '(1 2 3 4 5)))
       (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] )
       second
       (rescale 5)
       ;plot
       )
  (graph '(1 2 3 4))
  (apply max '(1 2 3 4))

    )

(enable-console-print!)
(def points
    (r/atom
      [10
       ]))

(defn rescale [max-y coll]
  (let [width 800
        height 600
        hh (/ height 2)
        ystep (/ height max-y)
        step (/ width (count coll) )]
    [step (reduce (fn [prevpts [p1 p2 idx]] (conj prevpts [(- height (* ystep p1)) (- height (* ystep p2)) (* step idx)])) [] coll)]))

(defn foo2 []
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))
(defn foo3 []
  (take! (go (let [response (<! (http/get "http://localhost:8080/bar"
                                          {:with-credentials? false}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %)))

(defn foo []
  (str (.getSeconds (js/Date.)) " " @points))

(defn get-point []
  (js->clj (.getSeconds (js/Date.))))


(defn add-point []
  (swap! points (fn [pts]
                  (conj pts (get-point)))))



(defonce timer (r/atom (js/Date.)))

(def time-color (r/atom "#920"))
(def test-field (r/atom "data..."))

(def time-updater (js/setInterval
                        #(do
                           ;(reset! test-field (get-point))
                           (add-point)
                           ;(reset! timer (js/Date.))
                           ) 2000))

(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))


(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defn test-component []
  [:div.color-input
   "test="
   @test-field
   ])

;;-------------------------------



(defonce slider
         (r/atom
           {:handle (g/point 500 50)
            :history []}))

(defn record-state [_ _ _ s]
  (swap! slider (fn [{:keys [history] :as coll}]
                  (assoc coll :history (conj history s)))))

(defn start-recording-history []
  (let [history (:history @slider)]
    (add-watch points :record record-state)))

(defn stop-recording-history []
  (remove-watch points :record))

(add-watch points :record record-state)

(defn get-bcr [svg-root]
  (-> svg-root
      r/dom-node
      .getBoundingClientRect))

(defn move-point [svg-root p]
  (fn [x y]
    (let [bcr (get-bcr svg-root)]
      (swap! points assoc p (g/point (- x (.-left bcr)) (- y (.-top bcr)))))))

(defn move-slider [svg-root p]
  (fn [x y]
    (let [new-x (-> (- x (.-left (get-bcr svg-root)))
                    (min 500)
                    (max 100))
          position (/ (- new-x 100)
                      (- 500 100))
          history (:history @slider)]
      (swap! slider assoc p (g/point new-x 50))
      (reset! points (nth history (int (* (dec (count history)) position)))))))

(defn plot [[step coll]]
  (mapcat (fn [[p1 p2 idx]] [(c/segment (g/point idx p1) (g/point (+ idx step) p2) idx)
                             ;(c/point {:on-drag (move-point svg-root :c)} (g/point idx p1))
                             (c/point {:on-drag nil} (g/point idx p1) idx)
                             ]
            ) coll))

(defn graph [pts]
  (->> (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts))
       (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] )
       second
       (rescale 60)
       plot
       )

  )

(defn root [svg-root]
  (let [pts @points]
    [:g
     ;[c/triangle p1 p2 p3]
     ;[c/circle p c]
     ;[c/segment (get pts 0) (get pts 1)]
     ;[c/segment (get pts 1) (get pts 2)]
     ;(c/segment (get pts 2) (get pts 3))
     (graph pts)
     ;(map (fn [[p1 p2]] (c/segment p1 p2)) (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts)))
     ;[c/segment (g/point 100 50) (g/point 500 50)]
     ;[c/rect {:on-drag (move-slider svg-root :handle)
     ;         :on-start stop-recording-history
     ;         :on-end start-recording-history} (:handle @slider)]
     ;[c/circle  (g/point 500 400) 4]
     ;(c/point {:on-drag nil} (g/point 100 400))
     ;[c/point {:on-drag (move-point svg-root :p1)} (g/point 150 (get pts 2))]
     ;[c/point {:on-drag (move-point svg-root :p2)} (get pts 3)]
     ;[c/point {:on-drag (move-point svg-root :p3)} (get pts 4)]
     ]))

(defn canvas [{:keys [width height]}]
  [:svg
   {:width (or width 800)
    :height (or height 600)
    :style {:border "1px solid black"}}
   [:text {:style {:-webkit-user-select "none"
                   :-moz-user-select "none"}
           :x 20 :y 20 :font-size 20}
    "The points are draggable and the slider controls history"]
   [root (r/current-component)]])


(defn simple-example [{:keys [width height] :as ctx}]
  [:div
   [greeting "Hello world2, it is now"]
   [clock]
   [color-input]
   [test-component]
   [canvas ctx]])

(defn ^:export run []
  (r/render [simple-example]
            (js/document.getElementById "app")))
