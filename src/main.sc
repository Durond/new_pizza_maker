require: slotfilling/slotFilling.sc
    module = sys.zb-common

theme: /

    state: Start
        intent!: /start
        q!: $regex</start>
        script:
            $session.order = {
                pizza_type: null, pizza_size: null,
                crust_type: null, address: null,
                phone: null, payment_method: null,
                status: "draft"
            };
        a: Здравствуйте! Я помогу заказать пиццу 🍕
        buttons:
            "Заказать пиццу" -> /OrderPizza
            "Помощь" -> /Help

    state: OrderPizza
        intent!: /order_pizza
        script:
            if (!$session.order) {
                $session.order = {
                    pizza_type: null, pizza_size: null,
                    crust_type: null, address: null,
                    phone: null, payment_method: null,
                    status: "draft"
                };
            }
            var p = $parseTree._pizza_type;
            var s = $parseTree._pizza_size;
            if (p) { $session.order.pizza_type = p.value; }
            if (s) { $session.order.pizza_size = s.value; }
        a: Отлично! Давайте оформим заказ.
        go!: /CheckPizzaType

    state: CheckPizzaType
        script:
            if ($session.order.pizza_type) {
                $reactions.transition("/CheckSize");
            } else {
                $reactions.transition("/AskPizzaType");
            }

    state: AskPizzaType
        a: Какую пиццу хотите?
        buttons:
            "Мясная" -> /SavePizzaType
            "Сырная" -> /SavePizzaType
            "Грибная" -> /SavePizzaType
            "Вегетарианская" -> /SavePizzaType

    state: SavePizzaType
        intent!: /choose_pizza
        q!: мясная
        q!: сырная
        q!: грибная
        q!: вегетарианская
        script:
            var p = $parseTree._pizza_type;
            if (p) {
                $session.order.pizza_type = p.value;
            } else {
                $session.order.pizza_type = $request.query.toLowerCase();
            }
        a: {{$session.order.pizza_type}} — отличный выбор!
        go!: /CheckSize

    state: CheckSize
        script:
            if ($session.order.pizza_size) {
                $reactions.transition("/CheckCrust");
            } else {
                $reactions.transition("/AskSize");
            }

    state: AskSize
        a: Какой размер пиццы?
        buttons:
            "15 см" -> /SaveSize
            "25 см" -> /SaveSize
            "30 см" -> /SaveSize
            "35 см" -> /SaveSize

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
        go!: /AskCrust

    state: AskCrust
        a: Какой борт?
        buttons:
            "Обычный" -> /SaveCrust
            "Сырный" -> /SaveCrust

    state: SaveCrust
        intent!: /choose_crust
        q!: обычный
        q!: сырный
        script:
            var c = $parseTree._crust_type;
            if (c) {
                $session.order.crust_type = c.value;
            } else {
                $session.order.crust_type = $request.query.toLowerCase();
            }
        a: Борт: {{$session.order.crust_type}}.
        go!: /AskAddress

    state: AskAddress
        a: Куда доставить?

    state: SaveAddress
        intent!: /set_address
        script:
            var a = $parseTree._address;
            if (a) {
                $session.order.address = a.value;
            } else {
                $session.order.address = $request.query;
            }
        a: Адрес: {{$session.order.address}}.
        go!: /AskPhone

    state: AskPhone
        a: Ваш номер телефона?

    state: SavePhone
        intent!: /set_phone
        script:
            var p = $parseTree._phone;
            if (p) {
                $session.order.phone = p.value;
            } else {
                $session.order.phone = $request.query;
            }
        a: Записал номер: {{$session.order.phone}}.
        go!: /AskPayment

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
        script:
            var pm = $parseTree._payment_method;
            if (pm) {
                $session.order.payment_method = pm.value;
            } else {
                $session.order.payment_method = $request.query.toLowerCase();
            }
        a: Способ оплаты: {{$session.order.payment_method}}.
        go!: /ConfirmOrder

    state: ConfirmOrder
        a: |
            Ваш заказ:
            🍕 Пицца: {{$session.order.pizza_type}}
            📏 Размер: {{$session.order.pizza_size}}
            🧀 Борт: {{$session.order.crust_type}}
            📍 Адрес: {{$session.order.address}}
            📞 Телефон: {{$session.order.phone}}
            💳 Оплата: {{$session.order.payment_method}}

            Всё верно?
        buttons:
            "Да, оформить" -> /PlaceOrder
            "Изменить размер" -> /ChangeSize
            "Изменить адрес" -> /AskAddress
            "Отмена" -> /Cancel

    state: ChangeSize
        intent!: /change_size
        a: Какой размер хотите?
        buttons:
            "15 см" -> /SaveSize
            "25 см" -> /SaveSize
            "30 см" -> /SaveSize
            "35 см" -> /SaveSize

    state: PlaceOrder
        intent!: /confirm_yes
        q!: да
        q!: да, оформить
        q!: подтверждаю
        q!: ок
        script:
            $session.order.status = "confirmed";
        a: Заказ оформлен! Мы позвоним в течение 5 минут.
        go!: /Thanks

    state: Thanks
        a: Спасибо за заказ! Хорошего дня 🍕
        script:
            $jsapi.stopSession();

    state: Cancel
        intent!: /cancel
        q!: отмена
        q!: отменить
        q!: стоп
        a: Заказ отменён. Возвращайтесь!
        script:
            $jsapi.stopSession();

    state: Reset
        intent!: /reset
        q!: заново
        q!: с начала
        q!: сбросить
        script:
            $jsapi.stopSession();
        a: Начинаем заново.
        go!: /Start

    state: Help
        intent!: /help
        a: |
            Я умею:
            • принимать заказ пиццы
            • выбирать размер и борт
            • оформлять доставку
            Просто напишите «хочу пиццу» или нажмите кнопку ниже.
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
        script:
            $jsapi.stopSession();

    state: NoMatch
        event!: noMatch
        a: Я не понял. Вы сказали: {{$request.query}}