package pt.ipt.dama.teacherslog.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pt.ipt.dama.teacherslog.data.local.AppDatabase
import pt.ipt.dama.teacherslog.data.model.*
import pt.ipt.dama.teacherslog.data.repository.TeacherRepository
import pt.ipt.dama.teacherslog.utils.LocaleManager

/**
 * Main ViewModel for managing academic data and UI state.
 */
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TeacherRepository
    
    private val _isDarkMode = MutableLiveData(true)
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    // Language state
    private val _currentLanguage = MutableLiveData(LocaleManager.getLanguage(application))
    val currentLanguage: LiveData<String> = _currentLanguage

    val allAcademicYears: LiveData<List<AcademicYear>>
    
    private val _selectedYearId = MutableLiveData<Int>()
    val selectedYearId: LiveData<Int> = _selectedYearId

    val subjectsWithStats: LiveData<List<SubjectWithStats>> = _selectedYearId.switchMap { id ->
        repository.getSubjectsWithStatsForYear(id).asLiveData()
    }

    val globalStats: LiveData<GlobalStats> = _selectedYearId.switchMap { id ->
        repository.getGlobalStatsForYear(id).asLiveData()
    }

    val studentsClassesForYear: LiveData<List<StudentsClassWithStats>> = _selectedYearId.switchMap { id ->
        repository.getAllStudentsClassesForYear(id).asLiveData()
    }

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        val teacherDao = database.teacherDao()
        repository = TeacherRepository(teacherDao)
        
        allAcademicYears = repository.allAcademicYears.asLiveData()
        
        viewModelScope.launch {
            repository.allAcademicYears.collectLatest { years ->
                if (years.isNotEmpty() && (_selectedYearId.value == null)) {
                    _selectedYearId.value = years.first().id
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun setLanguage(languageCode: String) {
        if (_currentLanguage.value != languageCode) {
            _currentLanguage.value = languageCode
        }
    }

    fun selectYear(yearId: Int) {
        _selectedYearId.value = yearId
    }

    fun addAcademicYear(yearName: String) {
        viewModelScope.launch {
            repository.insertAcademicYear(AcademicYear(year = yearName))
        }
    }

    fun deleteAcademicYear(year: AcademicYear) {
        viewModelScope.launch {
            repository.deleteAcademicYear(year)
        }
    }

    fun addSubject(name: String, iconName: String, colorHex: String) {
        val yearId = _selectedYearId.value ?: return
        viewModelScope.launch {
            repository.insertSubject(Subject(academicYearId = yearId, name = name, iconName = iconName, colorHex = colorHex))
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun getSubjectById(subjectId: Int): LiveData<Subject?> {
        return repository.getSubjectById(subjectId).asLiveData()
    }

    fun getStudentsClassesForSubject(subjectId: Int): LiveData<List<StudentsClassWithStats>> {
        return repository.getStudentsClassesForSubject(subjectId).asLiveData()
    }

    fun getStudentsWithGradesForStudentsClass(studentsClassId: Int): LiveData<List<StudentWithGrade>> {
        return repository.getStudentsWithGradesForStudentsClass(studentsClassId).asLiveData()
    }

    suspend fun getStudentById(studentId: Int): Student? {
        return repository.getStudentById(studentId)
    }

    private val _currentGrade = MutableLiveData<Grade?>()
    val currentGrade: LiveData<Grade?> = _currentGrade

    fun loadGradeForStudent(studentId: Int) {
        viewModelScope.launch {
            _currentGrade.value = repository.getGradeForStudent(studentId)
        }
    }

    fun saveGrade(grade: Grade) {
        viewModelScope.launch {
            repository.insertGrade(grade)
        }
    }
}

/**
 * Factory class to satisfy Android's ViewModel initialization requirements.
 */
class TeacherViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
