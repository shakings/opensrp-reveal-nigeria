package org.smartregister.reveal.contract;

import org.smartregister.reveal.model.OfflineMapModel;

import java.util.List;

public interface DownloadedOfflineMapsContract extends OfflineMapsFragmentContract {

    interface Presenter {

        void onDeleteDownloadMap(List<OfflineMapModel> offlineMapModels);

        void fetchOAsWithOfflineDownloads(List<String> locationIds);

        void onOAsWithOfflineDownloadsFetched(List<OfflineMapModel> downloadedOfflineMapModelList);
    }

    interface View  {

        void setDownloadedOfflineMapModelList(List<OfflineMapModel> downloadedOfflineMapModelList);

        void deleteDownloadedOfflineMaps();

    }

    interface Interactor {

        void fetchLocationsWithOfflineMapDownloads(List<String> locationIds);
    }
}
