# App Movil Stress

Aplicacion Android desarrollada en Kotlin para la monitorizacion del estres mediante el cuestionario PSS-14, almacenamiento local con SQLite y generacion de recomendaciones personalizadas.

## Requisitos previos

Antes de ejecutar el proyecto necesitas tener instalado:

- Android Studio
- JDK compatible con Android Studio
- Android SDK
- Un emulador Android configurado o un dispositivo fisico

## 1. Clonar el repositorio

Clona el proyecto y abre la carpeta raiz en Android Studio.

```bash
git clone <URL_DEL_REPOSITORIO>
```

## 2. Crear o revisar `local.properties`

Este archivo no se sube a GitHub y debe existir en la raiz del proyecto.

Ruta:

```text
local.properties
```

Contenido minimo:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=TU_API_KEY_DE_GEMINI
```

### Que significa cada valor

- `sdk.dir`: ruta local donde esta instalado tu Android SDK
- `GEMINI_API_KEY`: clave de la API de Gemini usada para generar recomendaciones personalizadas

## 3. Como conseguir la clave de Gemini

1. Entra en Google AI Studio:
   `https://aistudio.google.com/`
2. Crea una API key
3. Copia esa clave en `local.properties` en la linea:

```properties
GEMINI_API_KEY=TU_API_KEY_DE_GEMINI
```

## 4. Sincronizar el proyecto

Una vez abierto en Android Studio:

1. Espera a que Android Studio detecte Gradle
2. Pulsa `Sync Project with Gradle Files` si no se sincroniza automaticamente
3. Espera a que termine la descarga de dependencias

## 5. Ejecutar la aplicacion

1. Selecciona un emulador o conecta un movil con depuracion USB activada
2. En Android Studio, selecciona la configuracion `app`
3. Pulsa el boton `Run`


## 6. Primer uso de la app

Cuando la app se abra:

1. Registra un usuario
2. Inicia sesion
3. Realiza el cuestionario PSS-14
4. Consulta el resultado, la recomendacion y la evolucion del estres

## 7. Que pasa si no configuro Gemini

Si no añades `GEMINI_API_KEY`:

- la app seguira funcionando
- el cuestionario, el login y la base de datos local funcionaran normalmente
- las recomendaciones se generaran con el modo local de respaldo en lugar de Gemini

## 8. Archivos locales que no deben subirse

Este proyecto ignora automaticamente archivos sensibles o locales como:

- `local.properties`
- archivos de Android Studio dentro de `.idea/`
- claves `*.jks`, `*.keystore`


## 9. Posibles problemas

### El boton Run no aparece activo

- Asegurate de que el proyecto ha sincronizado bien Gradle
- Comprueba que has seleccionado la configuracion `app`
- Verifica que hay un emulador o dispositivo disponible

### Gemini no responde

La app puede seguir funcionando aunque Gemini falle temporalmente.

En ese caso:

- se usa la recomendacion local de respaldo
- revisa que `GEMINI_API_KEY` este bien escrita
- vuelve a compilar la app despues de modificar `local.properties`

### Cambie `local.properties` y no veo efecto

Si cambias la clave o la ruta del SDK:

1. sincroniza Gradle otra vez
2. recompila
3. reinstala la app en el dispositivo

## 10. Estructura general del proyecto

- `app/src/main/java/.../ui/`: pantallas principales
- `app/src/main/java/.../database/`: acceso a SQLite
- `app/src/main/java/.../service/`: sesion, preguntas PSS-14 y Gemini
- `app/src/main/java/.../model/`: modelos de datos
- `app/src/main/res/`: layouts y recursos visuales

## 11. Nota importante

La integracion de Gemini en este proyecto esta pensada para una demo academica.
