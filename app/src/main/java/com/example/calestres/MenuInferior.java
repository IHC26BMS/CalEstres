package com.example.calestres;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

final class MenuInferior {
    private MenuInferior() {
    }

    static void configurar(Activity activity, int pantallaSeleccionada) {
        View inicio = activity.findViewById(R.id.navInicio);
        View calendario = activity.findViewById(R.id.navCalendario);
        View exportaciones = activity.findViewById(R.id.navExportaciones);

        configurarElemento(activity, inicio, R.id.navInicioLabel, R.id.navInicioIcon,
                0, pantallaSeleccionada, MainActivity.class);
        configurarElemento(activity, calendario, R.id.navCalendarioLabel, R.id.navCalendarioIcon,
                1, pantallaSeleccionada, Calendario.class);
        configurarElemento(activity, exportaciones, R.id.navExportacionesLabel,
                R.id.navExportacionesIcon, 2, pantallaSeleccionada, Exportaciones.class);
    }

    private static void configurarElemento(Activity activity, View elemento, int etiquetaId,
                                            int iconoId, int posicion, int seleccionada,
                                            Class<? extends Activity> destino) {
        boolean estaSeleccionado = posicion == seleccionada;
        elemento.setSelected(estaSeleccionado);
        int color = activity.getColor(estaSeleccionado
                ? R.color.accent_selected : R.color.text_muted);
        ((TextView) activity.findViewById(etiquetaId)).setTextColor(color);
        View icono = activity.findViewById(iconoId);
        if (icono instanceof TextView) {
            ((TextView) icono).setTextColor(color);
        }
        elemento.setOnClickListener(view -> {
            if (posicion == seleccionada) return;
            activity.startActivity(new Intent(activity, destino));
            activity.finish();
        });
    }
}
