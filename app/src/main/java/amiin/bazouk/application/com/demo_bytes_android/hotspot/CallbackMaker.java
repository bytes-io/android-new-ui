package amiin.bazouk.application.com.demo_bytes_android.hotspot;

import android.content.Context;

import com.android.dx.Code;
import com.android.dx.DexMaker;
import com.android.dx.FieldId;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Created by jonro on 19/03/2018.
 */

public final class CallbackMaker {
    private Class<?> myTetheringCallbackClazz;
    private DexMaker dexMaker;

    CallbackMaker(Context c){
        Class callbackClass = null;
        try {
            callbackClass = Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        TypeId<?> systemCallbackTypeId = TypeId.get(callbackClass);

         dexMaker = new DexMaker();

        // Generate a TetheringCallback class.
        TypeId<?> tetheringCallback = TypeId.get("LTetheringCallback;");

        dexMaker.declare(tetheringCallback, "TetheringCallback.generated", Modifier.PUBLIC, systemCallbackTypeId);

        // Add (Our local normal-Java) callback as a field to the generated callback
        TypeId<Integer> t = TypeId.get(Integer.class);
        FieldId<?,?> myCallbackFieldId = tetheringCallback.getField(t, "callback");
        dexMaker.declare(myCallbackFieldId, Modifier.PRIVATE, null);


        generateConstructorWorking(tetheringCallback,systemCallbackTypeId);

        // Create the dex file and load it.
        File outputDir = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            outputDir = c.getCodeCacheDir();
        }

        try {
            ClassLoader loader = dexMaker.generateAndLoad(CallbackMaker.class.getClassLoader(),outputDir);
            myTetheringCallbackClazz = loader.loadClass("TetheringCallback");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private void generateConstructorWorking(TypeId<?> declaringType, TypeId<?> superType){
        final MethodId<?, ?> superConstructor = superType.getConstructor();

        MethodId<?, ?> constructor = declaringType.getConstructor(TypeId.INT);
        Code constructorCode = dexMaker.declare(constructor, Modifier.PUBLIC);
        final Local thisRef = constructorCode.getThis(declaringType);
        constructorCode.invokeDirect(superConstructor, null, thisRef);
        constructorCode.returnVoid();
    }

    public Class<?> getCallBackClass(){
        return myTetheringCallbackClazz;
    }




}
