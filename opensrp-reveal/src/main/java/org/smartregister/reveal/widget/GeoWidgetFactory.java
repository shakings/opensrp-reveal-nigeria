package org.smartregister.reveal.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.Point;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.gson.GeometryGeoJson;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.rengwuxian.materialedittext.validation.METValidator;
import com.rey.material.util.ViewUtil;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.interfaces.LifeCycleListener;
import com.vijay.jsonwizard.utils.ValidationStatus;
import com.vijay.jsonwizard.views.JsonFormFragmentView;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.reveal.R;
import org.smartregister.reveal.validators.MinZoomValidator;
import org.smartregister.reveal.view.RevealMapView;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.smartregister.reveal.util.Constants.JsonForm.OPERATIONAL_AREA_TAG;
import static org.smartregister.reveal.util.Constants.JsonForm.STRUCTURES_TAG;


/**
 * Created by samuelgithengi on 12/13/18.
 */
public class GeoWidgetFactory implements FormWidgetFactory, LifeCycleListener {

    private static final String TAG = "GeoWidgetFactory";

    private static final String ZOOM_LEVEL = "zoom_level";

    private static final String MAX_ZOOM_LEVEL = "v_zoom_max";

    private RevealMapView mapView;

    private JsonApi jsonApi;

    public static ValidationStatus validate(JsonFormFragmentView formFragmentView, RevealMapView mapView) {
        if (!Utils.isEmptyCollection(mapView.getValidators())) {
            for (METValidator validator : mapView.getValidators()) {
                if (validator instanceof MinZoomValidator) {
                    MapboxMap mapboxMap = mapView.getMapboxMap();
                    double zoom = mapView.getMapboxMap().getCameraPosition().zoom;
                    if (mapboxMap != null && !validator.isValid(String.valueOf(zoom), false)) {
                        Toast.makeText(formFragmentView.getContext(), validator.getErrorMessage(), Toast.LENGTH_LONG).show();
                        return new ValidationStatus(false, validator.getErrorMessage(), formFragmentView, mapView);
                    }
                }
            }
        }
        return new ValidationStatus(true, null, null, null);
    }

    @Override
    public List<View> getViewsFromJson(String stepName, Context context, JsonFormFragment formFragment, JSONObject jsonObject, CommonListener listener) throws Exception {
        return getViewsFromJson(stepName, context, formFragment, jsonObject, listener, false);
    }

    @Override
    public List<View> getViewsFromJson(String stepName, Context context, JsonFormFragment formFragment, JSONObject jsonObject, CommonListener listener, boolean popup) throws Exception {
        jsonApi = ((JsonApi) context);
        jsonApi.registerLifecycleListener(this);
        String openMrsEntityParent = jsonObject.optString(JsonFormConstants.OPENMRS_ENTITY_PARENT);
        String openMrsEntity = jsonObject.optString(JsonFormConstants.OPENMRS_ENTITY);
        String openMrsEntityId = jsonObject.optString(JsonFormConstants.OPENMRS_ENTITY_ID);
        String relevance = jsonObject.optString(JsonFormConstants.RELEVANCE);
        String key = jsonObject.optString(JsonFormConstants.KEY);


        List<View> views = new ArrayList<>(1);

        final int canvasId = ViewUtil.generateViewId();
        View rootLayout = LayoutInflater.from(context)
                .inflate(R.layout.item_geowidget, null);
        rootLayout.setId(canvasId);
        String operationalArea = null;
        String featureCollection = null;

        try {
            operationalArea = new JSONObject(formFragment.getCurrentJsonState()).optString(OPERATIONAL_AREA_TAG);
            featureCollection = new JSONObject(formFragment.getCurrentJsonState()).optString(STRUCTURES_TAG);
        } catch (JSONException e) {
            Log.e(TAG, "error extracting geojson form jsonform", e);
        }

        mapView = rootLayout.findViewById(R.id.geoWidgetMapView);
        mapView.onCreate(null);


        String finalOperationalArea = operationalArea;
        String finalFeatureCollection = featureCollection;
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                mapboxMap.setStyle(context.getString(R.string.reveal_satellite_style), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        GeoJsonSource geoJsonSource = style.getSourceAs(context.getString(R.string.reveal_datasource_name));

                        if (geoJsonSource != null && StringUtils.isNotBlank(finalFeatureCollection)) {
                            geoJsonSource.setGeoJson(finalFeatureCollection);
                        }
                    }
                });

                mapboxMap.getUiSettings().setRotateGesturesEnabled(false);

                mapView.setMapboxMap(mapboxMap);
                if (finalOperationalArea != null) {
                    CameraPosition cameraPosition = mapboxMap.getCameraForGeometry(GeometryGeoJson.fromJson(finalOperationalArea));
                    if (cameraPosition != null) {
                        mapboxMap.setCameraPosition(cameraPosition);
                    }
                } else {
                    mapView.focusOnUserLocation(true);
                }


                writeValues(((JsonApi) context), stepName, getCenterPoint(mapboxMap), key, openMrsEntityParent, openMrsEntity, openMrsEntityId, mapboxMap.getCameraPosition().zoom);
                mapboxMap.addOnMoveListener(new MapboxMap.OnMoveListener() {
                    @Override
                    public void onMoveBegin(@NonNull MoveGestureDetector detector) {//do nothing
                    }

                    @Override
                    public void onMove(@NonNull MoveGestureDetector detector) {//do nothing
                    }

                    @Override
                    public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                        Log.d(TAG, "onMoveEnd: " + mapboxMap.getCameraPosition().target.toString());
                        writeValues(((JsonApi) context), stepName, getCenterPoint(mapboxMap), key,
                                openMrsEntityParent, openMrsEntity, openMrsEntityId, mapboxMap.getCameraPosition().zoom);
                    }
                });
            }
        });

        JSONArray canvasIdsArray = new JSONArray();
        canvasIdsArray.put(canvasId);
        mapView.setTag(com.vijay.jsonwizard.R.id.canvas_ids, canvasIdsArray.toString());
        mapView.setTag(com.vijay.jsonwizard.R.id.address, stepName + ":" + jsonObject.getString(JsonFormConstants.KEY));
        mapView.setTag(com.vijay.jsonwizard.R.id.key, jsonObject.getString(JsonFormConstants.KEY));
        mapView.setTag(com.vijay.jsonwizard.R.id.openmrs_entity_parent, openMrsEntityParent);
        mapView.setTag(com.vijay.jsonwizard.R.id.openmrs_entity, openMrsEntity);
        mapView.setTag(com.vijay.jsonwizard.R.id.openmrs_entity_id, openMrsEntityId);
        mapView.setTag(com.vijay.jsonwizard.R.id.type, jsonObject.getString(JsonFormConstants.TYPE));
        if (relevance != null) {
            mapView.setTag(com.vijay.jsonwizard.R.id.relevance, relevance);
            ((JsonApi) context).addSkipLogicView(mapView);
        }

        ((JsonApi) context).addFormDataView(mapView);

        int screenHeightPixels = context.getResources().getDisplayMetrics().heightPixels;

        int editTextHeight = context.getResources().getDimensionPixelSize(R.dimen.native_form_edit_text_height);
        mapView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, screenHeightPixels - editTextHeight));

        addMaximumZoomLevel(jsonObject, mapView);
        views.add(rootLayout);
        mapView.onStart();

        mapView.showCurrentLocationBtn(true);
        mapView.enableAddPoint(true);
        ((JsonApi) context).onFormFinish();
        disableParentScroll((Activity) context, mapView);
        return views;
    }

    private void disableParentScroll(Activity context, View mapView) {
        ViewGroup mainScroll = context.findViewById(R.id.scroll_view);
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mainScroll.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

    }

    private void writeValues(JsonApi jsonApi, String stepName, Feature markerPosition, String key,
                             String openMrsEntityParent, String openMrsEntity, String openMrsEntityId, double zoomLevel) {
        if (markerPosition == null)
            return;
        try {
            jsonApi.writeValue(stepName, key, markerPosition.toJSON().toString(), openMrsEntityParent, openMrsEntity, openMrsEntityId, false);
            jsonApi.writeValue(stepName, ZOOM_LEVEL, zoomLevel + "", openMrsEntityParent, openMrsEntity, openMrsEntityId, false);
        } catch (JSONException e) {
            Log.e(TAG, "error writing Geowidget values", e);
        }

    }


    private Feature getCenterPoint(MapboxMap mapboxMap) {
        LatLng latLng = mapboxMap.getCameraPosition().target;
        Feature feature = new Feature();
        feature.setGeometry(new Point(latLng.getLatitude(), latLng.getLongitude()));
        return feature;
    }

    private void addMaximumZoomLevel(JSONObject jsonObject, RevealMapView mapView) {

        JSONObject minValidation = jsonObject.optJSONObject(MAX_ZOOM_LEVEL);
        if (minValidation != null) {
            try {
                mapView.addValidator(new MinZoomValidator(minValidation.getString(JsonFormConstants.ERR),
                        minValidation.getDouble(JsonFormConstants.VALUE)));
            } catch (JSONException e) {
                Log.e(TAG, "Error extracting max zoom level from" + minValidation);
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (mapView != null) {
            mapView.onCreate(bundle);
        }
    }

    @Override
    public void onStart() {
        if (mapView != null)
            mapView.onStart();
    }

    @Override
    public void onResume() {
        if (mapView != null)
            mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null)
            mapView.onPause();
    }

    @Override
    public void onStop() {
        if (mapView != null)
            mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (mapView != null)
            mapView.onSaveInstanceState(bundle);
    }

    @Override
    public void onLowMemory() {
        if (mapView != null)
            mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        jsonApi.unregisterLifecycleListener(this);
    }
}
