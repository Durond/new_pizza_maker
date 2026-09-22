    # Проверка: есть ли размер
    state: CheckSize
        script:
            if ($session.order.pizza_size) {
                $reactions.transition("/CheckCrust");
            } else {
                $reactions.transition("/AskSize");
            }

    # ---------- РАЗМЕР ----------
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