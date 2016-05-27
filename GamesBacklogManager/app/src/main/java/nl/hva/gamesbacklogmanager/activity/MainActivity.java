package nl.hva.gamesbacklogmanager.activity;

import android.app.ActivityOptions;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import nl.hva.gamesbacklogmanager.R;
import nl.hva.gamesbacklogmanager.R.id;
import nl.hva.gamesbacklogmanager.R.layout;
import nl.hva.gamesbacklogmanager.R.string;
import nl.hva.gamesbacklogmanager.adapter.GameListItemAdapter;
import nl.hva.gamesbacklogmanager.model.Game;
import nl.hva.gamesbacklogmanager.utility.ConfirmDeleteDialog;
import nl.hva.gamesbacklogmanager.utility.ConfirmDeleteDialog.ConfirmDeleteDialogListener;
import nl.hva.gamesbacklogmanager.utility.DBCRUD;


public class MainActivity extends AppCompatActivity implements ConfirmDeleteDialogListener {

    private GameListItemAdapter gameListItemAdapter;
    private List<Game> games;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.activity_game_main);
        setTitle(getString(string.title_screen_main));

        setListView();

        setFloatingActionButton();
    }

    private void setFloatingActionButton() {
        FloatingActionButton fab1 = (FloatingActionButton) findViewById(id.fab);
        fab1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddGameActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                }
                else {
                    startActivity(intent);
                }
            }
        });
    }

    private void setListView() {
        final RecyclerView gameList = (RecyclerView) findViewById(id.gameList);
        LayoutManager mLayoutManager = new LinearLayoutManager(this);
        gameList.setLayoutManager(mLayoutManager);

        // Create a DBCRUD object, and pass it the context of this activity
        DBCRUD dbcrud = new DBCRUD(this);
        // Get the list of games from Database
        games = dbcrud.getGames();

        gameListItemAdapter = new GameListItemAdapter(games, this);

        if (gameList != null) {
            gameList.setAdapter(gameListItemAdapter);
        }

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                if (viewHolder.getAdapterPosition() < target.getAdapterPosition()) {
                    for (int i = viewHolder.getAdapterPosition(); i < target.getAdapterPosition(); i++) {
                        Collections.swap(games, i, i + 1);
                    }
                } else {
                    for (int i = viewHolder.getAdapterPosition(); i > target.getAdapterPosition(); i--) {
                        Collections.swap(games, i, i - 1);
                    }
                }
                // Notify adapter Content has changed
                gameListItemAdapter.notifyDataSetChanged();
                // Create a DBCRUD object, and pass it the context of this activity
                DBCRUD dbcrud = new DBCRUD(MainActivity.this);
                // Delete the list of games from Database
                dbcrud.deleteAll();
                for(Game game : games)
                {
                    dbcrud.saveGame(game);
                }
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped item from list and notify the RecyclerView

                // Create a DBCRUD object, and pass it the context of this activity
                DBCRUD dbcrud = new DBCRUD(MainActivity.this);
                // Delete the list of games from Database
                dbcrud.deleteGame(viewHolder.getAdapterPosition());
                // Remove all games from temporary list
                games.remove(viewHolder.getAdapterPosition());
                // Display toast with Feedback
                showToast(getString(string.swipe_delete));
                // Notify adapter Content has changed
                gameListItemAdapter.notifyDataSetChanged();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);

        itemTouchHelper.attachToRecyclerView(gameList);

        gameList.setOnClickListener(new RecyclerView.OnClickListener() {
            @Override
            public void onClick(View view) {

                View parentRow = (View) view.getParent();
                ListView listView = (ListView) parentRow.getParent();
                int position = listView.getPositionForView(parentRow);

                Intent intent = new Intent(MainActivity.this, GameDetailsActivity.class);
                // Get the correct game based on which list item got clicked, and put it as parameter in the intent
                Game selectedGame = gameListItemAdapter.getItem(position);
                intent.putExtra("selectedGame", selectedGame);
                // Open GameDetailsActivity
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                }
                else {
                    startActivity(intent);
                }
            }


        });
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Create a DBCRUD object, and pass it the context of this activity
        DBCRUD dbcrud = new DBCRUD(this);
        // Delete the list of games from Database
        dbcrud.deleteAll();
        // Remove all games from temporary list
        games.removeAll(games);
        // Display toast with Feedback
        showToast(getString(string.action_database_clear));
        // Notify adapter Content has changed
        gameListItemAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // Do nothing, Dialog will disappear
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void showToast(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_delete_all) {
            // Show the ConfirmDeleteDialog
            DialogFragment dialog = new ConfirmDeleteDialog();
            Bundle bundle = new Bundle();
            bundle.putString("message", getString(string.dialog_game_deletion_all));
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "ConfirmDeleteDialog");
        }
        return super.onOptionsItemSelected(item);
    }
}

