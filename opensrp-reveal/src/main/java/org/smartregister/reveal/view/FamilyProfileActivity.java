package org.smartregister.reveal.view;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.smartregister.family.activity.BaseFamilyProfileActivity;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.family.util.Constants.INTENT_KEY;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.FamilyProfileContract;
import org.smartregister.reveal.fragment.FamilyProfileMemberFragment;
import org.smartregister.reveal.fragment.StructureTasksFragment;
import org.smartregister.reveal.model.FamilyProfileModel;
import org.smartregister.reveal.presenter.FamilyProfilePresenter;

import static org.smartregister.reveal.util.Constants.REQUEST_CODE_GET_JSON_FRAGMENT;

/**
 * Created by samuelgithengi on 2/8/19.
 */
public class FamilyProfileActivity extends BaseFamilyProfileActivity implements FamilyProfileContract.View {

    private StructureTasksFragment structureTasksFragment;

    private FamilyProfileMemberFragment profileMemberFragment;

    private String familyBaseEntityId;


    @Override
    protected void initializePresenter() {
        familyBaseEntityId = getIntent().getStringExtra(INTENT_KEY.FAMILY_BASE_ENTITY_ID);
        String familyHead = getIntent().getStringExtra(INTENT_KEY.FAMILY_HEAD);
        String primaryCaregiver = getIntent().getStringExtra(INTENT_KEY.PRIMARY_CAREGIVER);
        String familyName = getIntent().getStringExtra(INTENT_KEY.FAMILY_NAME);

        presenter = new FamilyProfilePresenter(this,
                new FamilyProfileModel(familyName), familyBaseEntityId, familyHead, primaryCaregiver, familyName);
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        profileMemberFragment = FamilyProfileMemberFragment.newInstance(this.getIntent().getExtras());
        adapter.addFragment(profileMemberFragment, this.getString(R.string.residents).toUpperCase());

        structureTasksFragment = StructureTasksFragment.newInstance(this.getIntent().getExtras(), this);
        adapter.addFragment(structureTasksFragment, this.getString(R.string.tasks, 0).toUpperCase());

        viewPager.setAdapter(adapter);

        return viewPager;
    }

    @Override
    public void setProfileImage(String baseEntityId) {
        //do nothing
    }

    @Override
    public void setStructureId(String structureId) {
        structureTasksFragment.setStructure(structureId);
    }

    @Override
    public void refreshTasks(String structureId) {
        structureTasksFragment.refreshTasks(structureId);
    }

    @Override
    public void updateFamilyName(String firstName) {
        if (profileMemberFragment.getArguments() != null) {
            profileMemberFragment.getArguments().putString(INTENT_KEY.FAMILY_NAME, firstName);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == io.ona.kujaku.utils.Constants.RequestCode.LOCATION_SETTINGS ||
                requestCode == REQUEST_CODE_GET_JSON_FRAGMENT) {
            structureTasksFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.edit_family == item.getItemId()) {
            startFormForEdit();
            return true;
        }
        if (R.id.add_member == item.getItemId()) {
            presenter().addFamilyMember();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void startFormForEdit() {
        if (familyBaseEntityId != null) {
            presenter().fetchProfileData();
        }
    }

    @Override
    public FamilyProfileContract.Presenter presenter() {
        return (FamilyProfileContract.Presenter) super.presenter();
    }
}
