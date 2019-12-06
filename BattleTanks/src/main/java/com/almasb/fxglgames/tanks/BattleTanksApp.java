/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2016 AlmasB (almaslvl@gmail.com)
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

package com.almasb.fxglgames.tanks;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.pathfinding.CellState;
import com.almasb.fxgl.pathfinding.astar.*;
import com.almasb.fxglgames.tanks.collision.BulletEnemyFlagHandler;
import com.almasb.fxglgames.tanks.collision.BulletEnemyTankHandler;
import com.almasb.fxglgames.tanks.components.TankViewComponent;
import com.almasb.fxglgames.tanks.components.ai.GuardComponent;
import com.almasb.fxglgames.tanks.components.ai.ShootPlayerComponent;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.tanks.BattleTanksType.*;
import static com.almasb.fxglgames.tanks.Config.*;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class BattleTanksApp extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("BattleTanks");
        settings.setVersion("0.2");
        settings.setWidth(60 * 21);
        settings.setHeight(60 * 12);
        settings.setExperimentalNative(true);
        settings.setDeveloperMenuEnabled(true);
    }

    private TankViewComponent tankViewComponent;

    private AStarGrid grid;

    @Override
    protected void initInput() {
        Input input = getInput();

        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                tankViewComponent.left();
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                tankViewComponent.right();
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                tankViewComponent.up();
            }
        }, KeyCode.W);

        input.addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                tankViewComponent.down();
            }
        }, KeyCode.S);

        input.addAction(new UserAction("Shoot") {
            @Override
            protected void onActionBegin() {
                tankViewComponent.shoot();
            }
        }, KeyCode.F);

        input.addAction(new UserAction("Move To") {
            @Override
            protected void onActionBegin() {

                tankViewComponent.getEntity().getComponent(AStarMoveComponent.class)
                        .moveToCell((int) (getInput().getMouseXWorld() / 30), (int) (getInput().getMouseYWorld() / 30));
                //tankViewComponent.getEntity().call("moveToCell", 2, 3);
            }
        }, KeyCode.G);
    }

    @Override
    protected void initGame() {
        getGameScene().setBackgroundColor(Color.LIGHTGRAY);

        getGameWorld().addEntityFactory(new BattleTanksFactory());

        setLevelFromMap("tmx/level2.tmx");

        // TODO: careful: the world itself is 21x12 but each block we count as 2
        grid = makeGridFromWorld(21*2, 12*2, BLOCK_SIZE / 2, BLOCK_SIZE / 2, (type) -> {
            if (type == WALL || type == BRICK || type == PLAYER_FLAG || type == ENEMY_FLAG)
                return CellState.NOT_WALKABLE;

            return CellState.WALKABLE;
        });

        var gridView = new AStarGridView(grid, BLOCK_SIZE / 2, BLOCK_SIZE / 2);
        getGameScene().addUINode(gridView);

        tankViewComponent = getGameWorld().getSingleton(PLAYER).getComponent(TankViewComponent.class);

        tankViewComponent.getEntity().addComponent(new AStarMoveComponent(new AStarPathfinder(grid)));

        byType(ENEMY).forEach(e -> e.addComponent(new AStarMoveComponent(new AStarPathfinder(grid))));

        //getGameWorld().getRandom(ENEMY).ifPresent(e -> e.addComponent(new GuardComponent(grid)));
        getGameWorld().getRandom(ENEMY).ifPresent(e -> e.addComponent(new ShootPlayerComponent()));
    }

    @Override
    protected void initPhysics() {
        var bulletTankHandler = new BulletEnemyTankHandler();

        getPhysicsWorld().addCollisionHandler(bulletTankHandler);
        getPhysicsWorld().addCollisionHandler(bulletTankHandler.copyFor(BULLET, PLAYER));

        var bulletFlagHandler = new BulletEnemyFlagHandler();

        getPhysicsWorld().addCollisionHandler(bulletFlagHandler);
        getPhysicsWorld().addCollisionHandler(bulletFlagHandler.copyFor(BULLET, PLAYER_FLAG));
    }

    private AStarGrid makeGridFromWorld(int worldWidth, int worldHeight, int cellWidth, int cellHeight,
                                        Function<Object, CellState> mapping) {

        var grid = new AStarGrid(worldWidth, worldHeight);
        grid.populate((x, y) -> {

            int worldX = x * cellWidth + cellWidth / 2;
            int worldY = y * cellHeight + cellHeight / 2;

            List<Object> collidingTypes = getGameWorld().getEntitiesInRange(new Rectangle2D(worldX-2, worldY-2, 4, 4))
                    .stream()
                    .map(Entity::getType)
                    .collect(Collectors.toList());

            boolean isWalkable = collidingTypes.stream()
                    .map(mapping)
                    .noneMatch(state -> state == CellState.NOT_WALKABLE);

            return new AStarCell(x, y, isWalkable ? CellState.WALKABLE : CellState.NOT_WALKABLE);
        });

        return grid;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
