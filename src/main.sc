require: slotfilling/slotFilling.sc
    module = sys.zb-common

theme: /

    # === СТАРТ ===
    state: Start
        q!: $regex</start>
        intent!: /start
        script:
            $session.order = {
                pizza_type: null,
                pizza_size: null,
                crust_type: null,
                address: null,
                phone: null,
                payment_method: null
            };
        a: Здравствуйте! Я помогу заказать пиццу.
        buttons:
            "Заказать пиццу" -> /OrderPizza

    # === ЗАКАЗ ПИЦЦЫ (главный поток) ===
    state: OrderPizza
        intent!: /order_pizza
        script:
            $session.order.pizza_type = $parseTree._pizza_type?.value;
            $session.order.pizza_size = $parseTree._pizza_size?.value;
        a: Отлично! Давайте оформим заказ.
        go!: /AskSize

    # === ЕСЛИ РАЗМЕР НЕ РАСПОЗНАН — СПРАШИВАЕМ ===
    state: AskSize
        a: Какой размер пиццы?
        buttons:
            "15 см" -> /SaveSize
            "25 см" -> /SaveSize
            "30 см" -> /SaveSize
            "35 см" -> /SaveSize
        state: SaveSize
            intent!: /choose_size
            script:
                $session.order.pizza_size = $parseTree._pizza_size?.value;
            a: Размер {{$session.order.pizza_size}} см — отлично!
            go!: /AskCrust
        state: SaveSizeByButton
            q!: 15 см
            q!: 25 см
            q!: 30 см
            q!: 35 см
            script:
                $session.order.pizza_size = $request.query;
            go!: /AskCrust

    # === ТИП БОРТА ===
    state: AskCrust
        a: Какой борт?
        buttons:
            "Обычный" -> /SaveCrust
            "Сырный" -> /SaveCrust
        state: SaveCrust
            intent!: /choose_crust
            script:
                $session.order.crust_type = $parseTree._crust_type?.value;
            a: Борт: {{$session.order.crust_type}}.
            go!: /AskAddress

    # === АДРЕС ===
    state: AskAddress
        a: Куда доставить?
        state: SaveAddress
            intent!: /set_address
            script:
                $session.order.address = $parseTree._address?.value || $request.query;
            a: Адрес: {{$session.order.address}}.
            go!: /AskPhone

    # === ТЕЛЕФОН ===
    state: AskPhone
        a: Ваш номер телефона?
        state: SavePhone
            intent!: /set_phone
            script:
                $session.order.phone = $parseTree._phone?.value || $request.query;
            go!: /AskPayment

    # === ОПЛАТА ===
    state: AskPayment
        a: Как будете оплачивать?
        buttons:
            "Картой" -> /ConfirmOrder
            "Наличными" -> /ConfirmOrder
        state: SavePayment
            intent!: /set_payment
            script:
                $session.order.payment_method = $parseTree._payment_method?.value;
            go!: /ConfirmOrder

    # === ПОДТВЕРЖДЕНИЕ ===
    state: ConfirmOrder
        a: |
            Ваш заказ:
            🍕 Пицца: {{$session.order.pizza_type}}
            📏 Размер: {{$session.order.pizza_size}} см
            🧀 Борт: {{$session.order.crust_type}}
            📍 Адрес: {{$session.order.address}}
            📞 Телефон: {{$session.order.phone}}
            💳 Оплата: {{$session.order.payment_method}}
            
            Всё верно?
        buttons:
            "Да" -> /PlaceOrder
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
        script:
            $session.order.status = "confirmed";
        a: Заказ оформлен! Мы позвоним в течение 5 минут.
        image: https://i.imgur.com/pizza-done.jpg
        go!: /Thanks

    state: Thanks
        a: Спасибо за заказ! Хорошего дня 🍕
        script:
            $jsapi.stopSession();

    # === ОТМЕНА ===
    state: Cancel
        intent!: /cancel
        a: Заказ отменён. Возвращайтесь!
        script:
            $jsapi.stopSession();

    # === NO MATCH ===
    state: NoMatch
        event!: noMatch
        a: Я не понял. Вы сказали: {{$request.query}}