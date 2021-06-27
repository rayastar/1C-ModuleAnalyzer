package db;


import utils.Utils;

import java.util.Map;

public class Method {

    public String base; // ЗУП
    public String className; // РегистрыСведений
    public String objectName; // Цены номенклатуры
    public String moduleType; // Модуль менеджера
    public String area; // Имя области
    public String paramHash; // Хэш параметров
    public String bodyHash; // Хэш текста
    public String directive = ""; // НаКлиенте

    public String moduleName; // СтроковыеФункцииКлиентСервер
    public String name; // РазложитьСтрокуВМассивПодстрок
    public String fullName = ""; // СтроковыеФункцииКлиентСервер.РазложитьСтрокуВМассивПодстрок
    public String filePath; // E:\1C\БСП\CommonModules\СтроковыеФункцииКлиентСервер\Ext\Module.bsl

    public boolean isExport; // Это экспортный метод
    public boolean isFunction; // Это функция

    public int begin; // Номер первой строки метода
    public int ending; // Номер последней строки метода
    public int countMethods; // кол-во вызовов
    public int uniqueCountMethods; // кол-во уникальных вызовов
    public int moduleLength; // // Длина иени модуля
    public int nameLength; // длина имети метода
    public int fullLength; // длина имени модуля + имени метода
    public int lineCount; // кол-во строк кода метода
    public int count; // кол-во вызовов данного метода во всей конфигурации
    public int parametersCount;

    public String parameters; // Параметры через запятую
    public String calledMethods; // Имена вызываемых методов
    public String uniqueCalledMethods; // Имена уникальнх вызываемых методов

    public Method(Map<String, String> info, Map<String, String> moduleInfo, int index, String filePath, String line, String area) {

        this.name = info.get("name");
        this.fullName = this.name;
        this.parameters = info.get("parameters");
        this.parametersCount = this.parameters.split(",").length;
        this.paramHash = Utils.MD5(this.parameters);
        this.isExport = info.get("isExport").equals("true");
        this.area = area;
        this.directive = info.get("directive");
        this.className = moduleInfo.get("className");
        this.objectName = moduleInfo.get("objectName");
        this.moduleType = moduleInfo.get("moduleType");
        this.moduleName = moduleInfo.get("moduleName");
        this.isFunction = !line.startsWith("Процедура");
        if (!this.moduleName.isEmpty() && this.isExport)
            this.fullName = this.moduleName + "." + this.name;

        this.fullLength = this.fullName.length();
        this.nameLength = this.name.length();
        this.moduleLength = this.moduleName.length();

        this.begin = index;
        this.filePath = filePath;
        this.base = this.filePath.split("\\\\")[2];
    }

}

