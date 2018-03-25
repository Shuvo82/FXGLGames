/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.spacerunner.control;

import com.almasb.fxgl.app.DSLKt;
import com.almasb.fxgl.app.FXGL;
import com.almasb.fxgl.entity.Control;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Required;
import com.almasb.fxgl.entity.Entities;
import com.almasb.fxgl.entity.component.PositionComponent;
import com.almasb.fxgl.time.LocalTimer;
import com.almasb.fxglgames.spacerunner.GameConfig;
import com.almasb.fxglgames.spacerunner.SpaceRunnerFactory;
import com.almasb.fxglgames.spacerunner.SpaceRunnerType;
import com.almasb.fxglgames.spacerunner.WeaponType;
import javafx.util.Duration;

import static com.almasb.fxgl.app.DSLKt.*;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class PlayerControl extends Control {

    private PositionComponent position;

    private double speed;

    private LocalTimer shootTimer = FXGL.newLocalTimer();
    private boolean canShoot = true;

    private WeaponType weapon = WeaponType.NORMAL;

    @Override
    public void onUpdate(Entity entity, double tpf) {
        speed = tpf * 300;

        position.translateX(tpf * FXGL.<GameConfig>getGameConfig().getPlayerSpeed());

        if (shootTimer.elapsed(Duration.seconds(0.12))) {
            canShoot = true;
        }
    }

    public void up() {
        position.translateY(-speed);
    }

    public void down() {
        position.translateY(speed);
    }

    public void changeWeapon() {
        weapon = WeaponType.LASER;
    }

    public void shoot() {
        if (geti("bullets") == 0)
            return;

        if (!canShoot)
            return;

        if (weapon == WeaponType.LASER) {

            spawn("Laser", getEntity().getCenter());

            inc("laser", -1);

        } else {

            spawn("Bullet", getEntity().getPosition());
            spawn("Bullet", getEntity().getPosition().add(0, getEntity().getHeight() - 10));

            inc("bullets", -1);
        }



        shootTimer.capture();
        canShoot = false;
    }
}
