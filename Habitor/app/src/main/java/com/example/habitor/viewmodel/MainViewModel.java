package com.example.habitor.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import java.util.List;

// ViewModel giúp quản lý data giữa các fragment
public class MainViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Habit>> habitsLiveData = new MutableLiveData<>();
    private final AppDatabase db;

    public MainViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        loadHabits();
    }

    public LiveData<List<Habit>> getHabits() {
        return habitsLiveData;
    }

    public void loadHabits() {
        new Thread(() -> {
            List<Habit> data = db.habitDao().getAll();
            habitsLiveData.postValue(data);
        }).start();
    }

    public void addHabit(Habit habit) {
        new Thread(() -> {
            db.habitDao().insert(habit);
            loadHabits();
        }).start();
    }

    public void deleteHabit(Habit habit) {
        new Thread(() -> {
            db.habitDao().moveToTrash(habit.id);
            loadHabits();
        }).start();
    }
}
