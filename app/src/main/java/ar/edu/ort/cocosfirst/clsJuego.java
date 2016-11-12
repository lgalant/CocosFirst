package ar.edu.ort.cocosfirst;


import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import org.cocos2d.actions.instant.CallFunc;
import org.cocos2d.actions.interval.DelayTime;
import org.cocos2d.actions.interval.MoveTo;
import org.cocos2d.actions.interval.ScaleBy;
import org.cocos2d.actions.interval.Sequence;
import org.cocos2d.layers.ColorLayer;
import org.cocos2d.layers.Layer;
import org.cocos2d.nodes.Director;
import org.cocos2d.nodes.Label;
import org.cocos2d.nodes.Scene;
import org.cocos2d.nodes.Sprite;
import org.cocos2d.opengl.CCGLSurfaceView;
import org.cocos2d.types.CCColor3B;
import org.cocos2d.types.CCColor4B;
import org.cocos2d.types.CCPoint;
import org.cocos2d.types.CCSize;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Leandro on 8/27/2016.
 */
public class clsJuego {

    CCGLSurfaceView _VistaDelJuego;
    CCSize PantallaDelPhone;
    Sprite NaveJugador;
    ArrayList<Sprite> enemigos;
    ArrayList<Sprite> vidas;
    int cantVidas=3;
    int enemigoLastTag=100;
    Timer relojEnemigos, relojColisiones;

    public clsJuego(CCGLSurfaceView VistaDelJuego) {
        _VistaDelJuego = VistaDelJuego;
    }

    public void comenzarJuego(){
        Director.sharedDirector().attachInView(_VistaDelJuego);
       // Director.sharedDirector().setAnimationInterval(1.0f/60);
       // Director.sharedDirector().setDisplayFPS(true);

        PantallaDelPhone = Director.sharedDirector().displaySize();
        Log.d("comenzarJuego","Ancho:" +PantallaDelPhone.width+ "-Alto:" + PantallaDelPhone.height);

        enemigos = new ArrayList<>();
        vidas = new ArrayList<>();

        Director.sharedDirector().runWithScene(EscenaDelJuego());
    }

    //Las escenas corresponden a cada pantalla del juego, de forma que tendremos una escena
    // para el juego en sí, otra para el Game Over, etc

    //Las capas son un componente de las escenas, y consisten en transparencias
    //superpuestas sobre las que se colocan los personajes.  Una escena puede tener
    //muchas capas, una tras otra.  Al ser transparentes, los elementos de cada capa
    //serán visibles solamente si la capa de más adelante no tiene un elemento justo
    //en la misma posición, tapándolo.

    private Scene EscenaDelJuego() {

        Scene escenaADevolver;
        escenaADevolver= Scene.node();

        CapaDeFondo cdfon = new CapaDeFondo();
        CapaDeFrente cdfre = new CapaDeFrente();

        //Cuando más chico es el número, más lejos de mis ojos estará.
        escenaADevolver.addChild(cdfon,-10);
        escenaADevolver.addChild(cdfre,10);

        return escenaADevolver;

    }


    private class CapaDeFondo extends Layer {

        public CapaDeFondo() {
            Log.d("CapaDeFondo","Constructor");
            PonerImagenFondo();
        }

        private void PonerImagenFondo() {
            Sprite imgFondo = Sprite.sprite("fondo.png");
            imgFondo.setPosition(PantallaDelPhone.width/2,PantallaDelPhone.height/2);
            imgFondo.runAction(ScaleBy.action(0.01f,3.0f,4.0f));
            addChild(imgFondo);
        }
    }

    private class CapaDeFrente extends Layer {
        Random random;
        int lastTagColision=0;
        public CapaDeFrente() {
            Log.d("CapaDeFrente", "Constructor");
            random = new Random();
            PonerNavePosInicial();
            PonerVidas();
            setIsTouchEnabled(true);

            TimerTask ponerEnemigosTask = new TimerTask() {
                @Override
                public void run() {
                    PonerEnemigo();
                }
            };

            relojEnemigos = new Timer();
            relojEnemigos.schedule(ponerEnemigosTask,0,2000);

            TimerTask detectColisionesTask = new TimerTask() {
                @Override
                public void run() {
                    detectarColisiones();
                }
            };
            relojColisiones = new Timer();
            relojColisiones.schedule(detectColisionesTask,0,500);

        }


        private void PonerNavePosInicial() {
            Log.d("PonerNavePosInicial","Comienzo1");
            NaveJugador = Sprite.sprite("rocket_mini.png");  // Tiene q haber fps_images en assets junto a este
            // La posición 0,0 corresponde al vértice inferior izquierdo de la pantalla. X crece hacia la derecha, e Y hacia arriba.

            float posIniX=PantallaDelPhone.width/2;
            float posIniY=PantallaDelPhone.height/2;
            NaveJugador.setPosition(posIniX,posIniY);
            addChild(NaveJugador,1,0);  // Escena -> Capa -> Sprite
        }

        private void PonerVidas() {
            Sprite vida;
            for (int i = 0; i < cantVidas; i++) {
                vida = Sprite.sprite("life2.png");
                vida.runAction(ScaleBy.action(0.01f, 0.3f, 0.3f));
                vida.setPosition(70+(70*i), 50);
                vidas.add(vida);
                addChild(vida, 1, i+1);
            }
        }


        private void PonerEnemigo() {
            Log.d("PonerEnemigo","Comienzo2");
            Sprite enemigo = Sprite.sprite("enemigo.gif");
            CCPoint posIni = new CCPoint();

            float AlturaEnemigo = enemigo.getHeight();
            posIni.y=PantallaDelPhone.height + AlturaEnemigo/2;
            posIni.x=random.nextInt((int)PantallaDelPhone.width);
            Log.d("PonerEnemigo","x:"+posIni.x+"-y:"+posIni.y);
            enemigo.setPosition(posIni.x,posIni.y);

            CCPoint posFin = new CCPoint();
            posFin.x = posIni.x;
            posFin.y=-AlturaEnemigo/2;

            enemigo.runAction(MoveTo.action(3,posFin.x,posFin.y));
            enemigos.add(enemigo);
            addChild(enemigo,1,enemigoLastTag++);

        }




        private synchronized void detectarColisiones() {
            for (Sprite enemigo:enemigos) {
                if (enemigo.getPositionY() <0) {
                    Log.d("tag",enemigo.getTag()+" esta abajo");
                    removeChild(enemigo.getTag(),true);
                    continue;
                }
                if (colisionSprites(enemigo,NaveJugador)) {
                    Log.d("detectarColisiones", "Colision!!! vidas:" + cantVidas);
                    if (enemigo.getTag() != lastTagColision) {
                        Log.d("detectarColisiones", "new tag!!!");
                        removeChild(cantVidas--, true);
                        lastTagColision = enemigo.getTag();
                        if (cantVidas <=0) {

                            if(relojEnemigos != null) {
                                relojEnemigos.cancel();
                                relojEnemigos.purge();
                                relojEnemigos = null;
                            }

                            if(relojColisiones != null) {
                                relojColisiones.cancel();
                                relojColisiones.purge();
                                relojColisiones = null;
                            }

                            cantVidas=3;
                            enemigos = new ArrayList<>();
                            vidas = new ArrayList<>();
                            removeAllChildren(true);
                            Scene gameOverScene = Scene.node();
                            GameOverLayer gol = new GameOverLayer(new CCColor4B(255, 255, 255, 255));
                            gol.getLabel().setString("Perdiste");
                            gameOverScene.addChild(gol);

                            Director.sharedDirector().replaceScene(gameOverScene);
                        }
                    }
                }

            }

        }


        // Colisiones

        private boolean isBetween(int nro, int desde, int hasta) {
            if (desde > hasta) {
                int aux;
                aux =desde;
                desde= hasta;
                hasta = aux;
            }
            return (nro >= desde && nro <=hasta);
        }

        private boolean colisionSprites(Sprite s1, Sprite s2) {
            int s1I, s1D, s1Ab, s1Arr;
            int s2I, s2D, s2Ab, s2Arr;
            boolean devolver = false;

            s1I = (int) (s1.getPositionX() - s1.getWidth()/2);
            s1D = (int) (s1.getPositionX() + s1.getWidth()/2);
            s1Ab = (int) (s1.getPositionY() - s1.getHeight()/2);
            s1Arr = (int) (s1.getPositionY() + s1.getHeight()/2);
            Rect r1 = new Rect(s1I,s1Ab,s1D,s1Arr); // Ojo se invierte abajo y arriba porque android lo ve alreves!!!!

            s2I = (int) (s2.getPositionX() - s2.getWidth()/2);
            s2D = (int) (s2.getPositionX() + s2.getWidth()/2);
            s2Ab = (int) (s2.getPositionY() - s2.getHeight()/2);
            s2Arr = (int) (s2.getPositionY() + s2.getHeight()/2);
            Rect r2 = new Rect(s2I,s2Ab,s2D,s2Arr);


            return r1.intersect(r2);
            /*
            Log.d("S1 tag:",s1.getTag()+" i:"+s1I+" ar:"+s1Arr+" d:"+s1D+" ab:"+s1Ab);
            Log.d("S2","i:"+s2I+" ar:"+s2Arr+" d:"+s2D+" ab:"+s2Ab);
            Log.d("intersect1",":"+r1.intersect(r2));
            Log.d("intersect2",":"+r2.intersect(r1));
*/


/*
            //Borde izq y borde inf de Sprite 1 está dentro de Sprite 2
            if (isBetween(s1I,s2I,s2D) && isBetween(s1Ab,s2Ab, s2Arr))
                devolver = true;

            //Borde izq y borde sup de Sprite 1 está dentro de Sprite 2
            if (isBetween(s1I,s2I,s2D) && isBetween(s1Arr,s2Ab, s2Arr))
                devolver = true;

            //Borde der y borde sup de Sprite 1 está dentro de Sprite 2
            if (isBetween(s1D,s2I,s2D) && isBetween(s1Arr,s2Ab, s2Arr))
                devolver = true;

            //Borde der y borde inf de Sprite 1 está dentro de Sprite 2
            if (isBetween(s1D,s2I,s2D) && isBetween(s1Ab,s2Ab, s2Arr))
                devolver = true;




            //Borde izq y borde inf de Sprite 2 está dentro de Sprite 1
            if (isBetween(s2I,s1I,s1D) && isBetween(s2Ab,s1Ab, s1Arr))
                devolver = true;

            //Borde izq y borde sup de Sprite 2 está dentro de Sprite 1
            if (isBetween(s2I,s1I,s1D) && isBetween(s2Arr,s1Ab, s1Arr))
                devolver = true;

            //Borde der y borde sup de Sprite 2 está dentro de Sprite 1
            if (isBetween(s2D,s1I,s1D) && isBetween(s2Arr,s1Ab, s1Arr))
                devolver = true;

            //Borde der y borde inf de Sprite 2 está dentro de Sprite 1
            if (isBetween(s2D,s1I,s1D) && isBetween(s2Ab,s1Ab, s1Arr))
                devolver = true;

            return devolver;
            */
        }

        // Touches
        //Cuál de las tres usaremos? Eso va a depender del tipo de juego que estemos
        //haciendo, y en especial, de la forma que queramos darle al control de dicho
        //juego.  Por ejemplo, si estamos detectando el toque de un botón de disparo,
        //solo nos interesa el momento en que el toque comienza, sin importarnos si
        //desplaza su dedo.  En el momento en que el toque comienza, nosotros ya
        //procesamos el disparo.

        //En cambio, si queremos trazar una trayectoria por la pantalla, e ir recopilando
        //cada punto por el cual se desplazó, usaremos el controlador de desplazamiento.
        @Override
        public boolean ccTouchesBegan(MotionEvent event)
        {
            //En el instante en que se detecta que es tocado, el sistema operativo invoca a esta función,
            //Hasta que el usuario no despegue su dedo de la pantalla, y la vuelva a tocar, esta función
            //no será invocada nuevamente.
            return super.ccTouchesBegan(event);
        }

        @Override
        public boolean ccTouchesMoved(MotionEvent event) {
            //cada vez que el usuario desplaza su dedo desde una posición anterior a una posición nueva,
            //sin dejar de tocar la pantalla.

            //Cocos2D toma como  punto 0,0 el vértice inferior izquierdo de la pantalla. Pero el touch toma como
            //punto 0,0 el vértice superior izquierdo.
            MoverNaveJugador(event.getX(),PantallaDelPhone.getHeight() - event.getY());
            return true;
        }


        @Override
        public boolean ccTouchesEnded(MotionEvent event) {
            //indica que el usuario despegó su dedo de la pantalla,
            return super.ccTouchesEnded(event);
        }

        private void MoverNaveJugador(float x, float y) {
            // NaveJugador.setPosition(x,y); esto hace que se teletransporte

            // vamos a determinar a qué distancia del centro de la pantalla se tocó.  Si tocó del centro hacia la derecha,
            // la nave irá hacia la derecha.  Si tocó del centro hacia arriba, irá hacia arriba.  Ambas direcciones
            // pueden ser combinadas. Y cuánto más lejos del centro se toque más rápida será la velocidad de desplazamiento.

            float movX, movY;
            movX = x - PantallaDelPhone.getWidth()/2;
            movY = y - PantallaDelPhone.getHeight()/2;

            // Suavizo movimiento
            movX = movX / 20;
            movY = movY / 20;

            float posFinx = NaveJugador.getPositionX()+movX;
            float posFiny = NaveJugador.getPositionY()+movY;

            if (posFinx<NaveJugador.getWidth()/2)
                posFinx=NaveJugador.getWidth()/2;

            if (posFinx>PantallaDelPhone.getWidth()-NaveJugador.getWidth()/2)
                posFinx=PantallaDelPhone.getWidth()-NaveJugador.getWidth()/2;

            if (posFiny<NaveJugador.getHeight()/2)
                posFiny=NaveJugador.getHeight()/2;

            if (posFiny>PantallaDelPhone.getHeight()-NaveJugador.getHeight()/2)
                posFiny=PantallaDelPhone.getHeight()-NaveJugador.getHeight()/2;

            NaveJugador.setPosition(posFinx, posFiny);



        }



    }


    private class GameOverLayer extends ColorLayer {
        protected Label _label;
        public GameOverLayer(CCColor4B color) {
            super(color);
            this.setIsTouchEnabled(true);
            _label = Label.label("Won't see me","DroidSans",32);
            _label.setColor(new CCColor3B(0,0,0));
            _label.setPosition(PantallaDelPhone.width/2.0f,PantallaDelPhone.height/2.0f);
            addChild(_label);
            this.runAction(Sequence.actions(DelayTime.action(3.0f), CallFunc.action(this,"gameOverDone")));
        }

        public void gameOverDone() {
            Director.sharedDirector().replaceScene(EscenaDelJuego());
        }

        @Override
        public boolean ccTouchesEnded(MotionEvent event)
        {
            gameOverDone();

            return true;
        }


        public Label getLabel()
        {
            return _label;
        }

    }
 }
