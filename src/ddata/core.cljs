(ns ddata.core
  (:require [clojure.string :refer [split]]
            [reagent.interop :refer-macros [$ $!]]
            [datascript.core :as d]))


(def db (d/create-conn {:entity {:db/unique :db.unique/identity}}))


(d/transact! db
  [{:entity     :skeleton
    :map-loaded false
    :img-loaded false
    :frame      0}
   {:entity     :swguy
    :map-loaded false
    :img-loaded false
    :frame      0}])


(def state
  (atom {:skeleton {:img nil
                    :map nil}
         :swguy    {:img nil
                    :map nil}}))


(def game-loop (atom nil))


(defn set-img-loaded! [name img]
  (let [entity-id (:db/id (d/entity @db [:entity name]))]
    (swap! state assoc-in [name :img] img)
    (d/transact! db [[:db/add entity-id :img-loaded true]])))


(defn set-asset-loaded! [name img-map]
  (let [entity-id (:db/id (d/entity @db [:entity name]))]
    (swap! state assoc-in [name :map] img-map)
    (d/transact! db [[:db/add entity-id :map-loaded true]])))


(defn get-canvas []
  ($ js/document querySelector "#app"))


(defn load-image [url]
  (let [img (js/Image.)]
    (js/Promise.
      (fn [res rej]
        ($! img :onload #(res img))
        ($! img :onerror #(rej %))

        ($! img :src url)))))


(defn load-asset [url]
  (-> (js/fetch url)
      ($ then #(.json %))
      ($ then #(js->clj % :keywordize-keys true))))


(defn draw-image [ctx img & {:keys [sx sy sw sh dx dy dw dh]}]
  ($ ctx drawImage img sx sy sw sh dx dy dw dh))


(defn render-image [ctx entity-state curr-frame y-offset]
  (let [image-name (get-in entity-state [:map :meta :image])
        [frame-prefix frame-suffix] (split image-name #"\.")
        frame-name (keyword (str frame-prefix curr-frame "." frame-suffix))
        frame      (get-in entity-state [:map :frames frame-name :frame])]
    (draw-image ctx (:img entity-state)
      :sx (:x frame)
      :sy (:y frame)
      :sw (:w frame)
      :sh (:h frame)
      :dx 100
      :dy y-offset
      :dw (:w frame)
      :dh (:h frame))))


(defn check-load-progress []
  (let [loaded (d/q '[:find [?img-loaded ?map-loaded]
                      :where
                      [_ :img-loaded ?img-loaded]
                      [_ :map-loaded ?map-loaded]]
                 @db)]
    (when (every? true? loaded)
      (swap! state update :fully-loaded not))))


(defn update-game []
  (let [skeleton (d/entity @db [:entity :skeleton])
        swguy    (d/entity @db [:entity :swguy])
        s-eid    (:db/id skeleton)
        s-frame  (:frame skeleton)
        sw-eid   (:db/id swguy)
        sw-frame (:frame swguy)]
    (d/transact! db [[:db/add s-eid :frame (mod (inc s-frame) 13)]
                     [:db/add sw-eid :frame (mod (inc sw-frame) 4)]])))


(defn render-game []
  (let [ctx (get @state :ctx)]
    ($ ctx clearRect 0 0 (.-innerWidth js/window) (.-innerHeight js/window))
    ($! ctx :fillStyle "grey")
    ($ ctx fillRect 0 0 (.-innerWidth js/window) (.-innerHeight js/window))

    (let [{:keys [skeleton swguy]} @state
          skeleton-frame (:frame (d/entity @db [:entity :skeleton]))
          swguy-frame    (:frame (d/entity @db [:entity :swguy]))]
      (render-image ctx skeleton skeleton-frame 50)
      (render-image ctx swguy swguy-frame 250))

    (update-game)))


(defn render []
  (let [fully-loaded (get @state :fully-loaded)]
    (if-not fully-loaded
      (check-load-progress)
      (render-game))))


(defn start []
  (let [canvas     (get-canvas)
        ctx        ($ canvas getContext "2d")
        frame-rate (/ 1000 20)]
    ($! canvas :width ($ js/window :innerWidth))
    ($! canvas :height ($ js/window :innerHeight))

    (-> (load-image "/img/skeleton-walk.png")
        ($ then (partial set-img-loaded! :skeleton)))

    (-> (load-image "/img/swguy_run.png")
        ($ then (partial set-img-loaded! :swguy)))

    (-> (load-asset "/assets/skeleton-walk.json")
        ($ then (partial set-asset-loaded! :skeleton)))

    (-> (load-asset "/assets/swguy_run.json")
        ($ then (partial set-asset-loaded! :swguy)))

    (swap! state assoc :ctx ctx)

    (reset! game-loop (js/setInterval render frame-rate))))


(defn stop []
  (when-let [loop @game-loop]
    (js/clearInterval loop)))


(defn ^:export init []
  (start))

