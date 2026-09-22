require: slotfilling/slotFilling.sc
    module = sys.zb-common

init:
    $global.pizza_prices = {
        "15": 300, "25": 500, "30": 700, "35": 900
    };
    $global.crust_prices = {
        "обычный": 0, "сырный": 100, "колбасный": 150
    };

theme: /

    # =========================
    # ПРИВЕТСТВИЕ / НАЧАЛО
    # =========================
    state: StartState
        q!: $regex</start>
        intent!: /start
        script:
            $session.order = {
                pizza_type: null,
                pizza_size: null,
                crust_type: null,
                address: null,
                payment_method: null,
                status: "draft",
                total: 0
            };
        random:
            a: Здравствуйте! Я помогу заказать пиццу 🍕
            a: Привет! Готов принять ваш заказ на пиццу 🍕
            a: Добрый день! Давайте закажем пиццу 🍕
        buttons:
            "Заказать пиццу" -> /OrderPizza
            "Помощь" -> /Help
            "Сбросить" -> /Reset

    # =========================
    # ЗАКАЗ — ВЛОЖЕННЫЕ СОСТОЯНИЯ
    # =========================
    state: OrderPizza
        intent!: /order_pizza
        script:
            if (!$session.order) {
                $session.order = {
                    pizza_type: null, pizza_size: null,
                    crust_type: null, address: null,
                    payment_method: null, status: "draft", total: 0
                };
            }
            var p = $parseTree._pizza_type;
            var s = $parseTree._pizza_size;
            if (p) { $session.order.pizza_type = p.value; }
            if (s) { $session.order.pizza_size = s.value; }
        a: Отлично! Давайте оформим заказ.
        go!: /OrderPizza/CheckPizzaType

        # ----- Проверка типа пиццы -----
        state: CheckPizzaType
            script:
                if ($session.order.pizza_type) {
                    $reactions.transition("/OrderPizza/CheckSize");
                } else {
                    $reactions.transition("/OrderPizza/AskPizzaType");
                }

        # ----- Спрашиваем тип -----
        state: AskPizzaType
            a: Какую пиццу хотите?
            buttons:
                "Мясная" -> /OrderPizza/SavePizzaType
                "Сырная" -> /OrderPizza/SavePizzaType
                "Грибная" -> /OrderPizza/SavePizzaType
                "Вегетарианская" -> /OrderPizza/SavePizzaType
                "Гавайская" -> /OrderPizza/SavePizzaType

        # ----- Сохранение типа -----
        state: SavePizzaType
            intent!: /choose_pizza
            q!: мясная
            q!: сырная
            q!: грибная
            q!: вегетарианская
            q!: гавайская
            script:
                var p = $parseTree._pizza_type;
                if (p) {
                    $session.order.pizza_type = p.value;
                } else {
                    $session.order.pizza_type = $request.query.toLowerCase();
                }
            a: {{$session.order.pizza_type}} — отличный выбор!
            go!: /OrderPizza/CheckSize

        # ----- Проверка размера -----
        state: CheckSize
            script:
                if ($session.order.pizza_size) {
                    $reactions.transition("/OrderPizza/CheckCrust");
                } else {
                    $reactions.transition("/OrderPizza/AskSize");
                }

        # ----- Спрашиваем размер -----
        state: AskSize
            a: Какой размер пиццы?
            buttons:
                "15 см" -> /OrderPizza/SaveSize
                "25 см" -> /OrderPizza/SaveSize
                "30 см" -> /OrderPizza/SaveSize
                "35 см" -> /OrderPizza/SaveSize

        state: SaveSize
            intent!: /choose_size
            q!: 15 см
            q!: 25 см
            q!: 30 см
            q!: 35 см
            script:
                var s = $parseTree._pizza_size;
                if (s) {
                    $session.order.pizza_size = s.value;
                } else {
                    $session.order.pizza_size = $request.query;
                }
            a: Размер {{$session.order.pizza_size}} — отлично!
            go!: /OrderPizza/CheckCrust

        # ----- Проверка борта -----
        state: CheckCrust
            script:
                if ($session.order.crust_type) {
                    $reactions.transition("/OrderPizza/AskAddress");
                } else {
                    $reactions.transition("/OrderPizza/AskCrust");
                }

        # ----- Спрашиваем борт -----
        state: AskCrust
            a: Какой борт?
            buttons:
                "Обычный" -> /OrderPizza/SaveCrust
                "Сырный" -> /OrderPizza/SaveCrust
                "Колбасный" -> /OrderPizza/SaveCrust

        state: SaveCrust
            intent!: /choose_crust
            q!: обычный
            q!: сырный
            q!: колбасный
            script:
                var c = $parseTree._crust_type;
                if (c) {
                    $session.order.crust_type = c.value;
                } else {
                    $session.order.crust_type = $request.query.toLowerCase();
                }
            a: Борт: {{$session.order.crust_type}}.
            go!: /OrderPizza/AskAddress

        # ----- Адрес: устанавливаем флаг и уходим в приёмник -----
        state: AskAddress
            script:
                $session.order.status = "waiting_address";
            a: Куда доставить?
            go!: /WaitInput

    # =========================
    # УНИВЕРСАЛЬНЫЙ ПРИЁМНИК АДРЕСА
    # =========================
    state: WaitInput
        q!: *
        script:
            $session.order.address = $request.query;
            $session.order.status = "draft";
            $reactions.transition("/AskPayment");

    # =========================
    # ОПЛАТА
    # =========================
    state: AskPayment
        a: Как будете оплачивать?
        buttons:
            "Картой" -> /SavePayment
            "Наличными" -> /SavePayment
            "Онлайн" -> /SavePayment

    state: SavePayment
        intent!: /set_payment
        q!: картой
        q!: карта
        q!: наличными
        q!: наличные
        q!: онлайн
        q!: *
        script:
            var pm = $parseTree._payment_method;
            if (pm) {
                $session.order.payment_method = pm.value;
            } else {
                $session.order.payment_method = $request.query.toLowerCase();
            }
        a: Способ оплаты: {{$session.order.payment_method}}.
        go!: /ConfirmOrder

    # =========================
    # ПОДТВЕРЖДЕНИЕ ЗАКАЗА
    # =========================
    state: ConfirmOrder
        script:
            var size = $session.order.pizza_size;
            var crust = $session.order.crust_type;
            var base = $global.pizza_prices[size] || 0;
            var crustPrice = $global.crust_prices[crust] || 0;
            $session.order.total = base + crustPrice;
        a: |
            Ваш заказ:
            🍕 Пицца: {{$session.order.pizza_type}}
            📏 Размер: {{$session.order.pizza_size}} см
            🧀 Борт: {{$session.order.crust_type}}
            📍 Адрес: {{$session.order.address}}
            💳 Оплата: {{$session.order.payment_method}}
            💰 Итого: {{$session.order.total}} ₽

            Всё верно?
        buttons:
            "Да, оформить" -> /ConfirmOrder/PlaceOrder
            "Изменить размер" -> /ChangeSize
            "Изменить адрес" -> /OrderPizza/AskAddress
            "Отмена" -> /Cancel

        # ----- Подтверждение (вложено) -----
        state: PlaceOrder
            intent!: /confirm_yes
            q!: да
            q!: да, оформить
            q!: верно
            q!: подтверждаю
            q!: ок
            script:
                $session.order.status = "confirmed";
            a: Заказ оформлен! Мы позвоним в течение 5 минут.
            go!: /OrderInProgress

    # =========================
    # ИМИТАЦИЯ ВЫПОЛНЕНИЯ ЗАКАЗА
    # =========================
    state: OrderInProgress
        a: Готовим вашу пиццу... 🍕
        timeout: /OrderReady || interval = 3

    state: OrderReady
        a: Ваш заказ готов! Курьер выехал по адресу {{$session.order.address}}.
        image: https://upload.wikimedia.org/wikipedia/commons/thumb/a/a3/Eq_it-na_pizza-margherita_sep2005_sml.jpg/320px-Eq_it-na_pizza-margherita_sep2005_sml.jpg
        go!: /Thanks

    # =========================
    # ИЗМЕНЕНИЕ ЗАКАЗА
    # =========================
    state: ChangeSize
        intent!: /change_size
        a: Какой размер хотите?
        buttons:
            "15 см" -> /OrderPizza/SaveSize
            "25 см" -> /OrderPizza/SaveSize
            "30 см" -> /OrderPizza/SaveSize
            "35 см" -> /OrderPizza/SaveSize

    # =========================
    # ФИНАЛ
    # =========================
    state: Thanks
        a: Спасибо за заказ! Хорошего дня 🍕
        go!: /Reset

    # =========================
    # СБРОС СЕССИИ
    # =========================
    state: Reset
        intent!: /reset
        q!: заново
        q!: с начала
        q!: сбросить
        script:
            $session.order = null;
            $jsapi.stopSession();
        a: Начинаем заново. Напишите «привет».
        go!: /StartState

    # =========================
    # ОТМЕНА
    # =========================
    state: Cancel
        intent!: /cancel
        q!: отмена
        q!: отменить
        q!: стоп
        a: Заказ отменён. Возвращайтесь!
        go!: /Reset

    # =========================
    # ПОМОЩЬ / СПАСИБО / ПОКА
    # =========================
    state: Help
        intent!: /help
        a: |
            Я умею:
            • принимать заказ пиццы
            • выбирать размер и борт
            • оформлять доставку
            • менять заказ
            Напишите «хочу пиццу» или нажмите кнопку.
        buttons:
            "Заказать пиццу" -> /OrderPizza

    state: Thanks2
        intent!: /thanks
        q!: спасибо
        q!: благодарю
        a: Пожалуйста! Всегда рад помочь 🍕

    state: Bye
        intent!: /bye
        q!: пока
        q!: до свидания
        a: До свидания! Ждём вас снова.
        go!: /Reset

    # =========================
    # NO MATCH
    # =========================
    state: NoMatch
        event!: noMatch
        if: !($session.order && $session.order.status == "waiting_address")
        a: Я не понял. Вы сказали: {{$request.query}}